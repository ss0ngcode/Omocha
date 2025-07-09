package org.omocha.domain.conclude;

import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omocha.domain.auction.Auction;
import org.omocha.domain.auction.AuctionReader;
import org.omocha.domain.auction.vo.Price;
import org.omocha.domain.bid.Bid;
import org.omocha.domain.bid.BidReader;
import org.omocha.domain.member.Member;
import org.omocha.util.DatabaseCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
public class ConcludeProcessorRetryTest {

	@SpringBootApplication                                // 테스트용 스프링 부트 애플리케이션 설정
	@EnableRetry
	@EntityScan(basePackages = "org.omocha.domain")        // 도메인 패키지 스캔
	@Import(DatabaseCleaner.class)                        // DatabaseCleaner 빈 등록
	static class TestApplication {
	}

	@Autowired
	private DatabaseCleaner databaseCleaner;
	@Autowired
	private ConcludeProcessor concludeProcessor;
	@MockBean
	private AuctionReader auctionReader;
	@MockBean
	private BidReader bidReader;
	@MockBean
	private ConcludeStore concludeStore;

	private Auction auction;
	private Bid highestBid;
	private Member buyer;
	private final Long AUCTION_ID = 1L;

	// 테스트를 위한 엔티티 초기 설정
	@BeforeEach
	void setUp() {
		buyer = Member.builder()
			.build();

		auction = Auction.builder()
			.memberId(1L)
			.title("테스트 경매")
			.startDate(LocalDateTime.now().minusDays(1))
			.endDate(LocalDateTime.now().plusDays(1))
			.startPrice(new Price(1000))
			.build();

		highestBid = Bid.builder()
			.auction(auction)
			.buyer(buyer)
			.bidPrice(new Price(2000))
			.build();
	}

	// 테스트 후 데이터베이스 초기화
	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("첫 시도에 성공하면 재시도 없이 낙찰 처리를 완료한다")
	void processConclusion_SuccessOnFirstAttempt() {
		// given
		given(auctionReader.getAuction(AUCTION_ID)).willReturn(auction);
		given(bidReader.findHighestBid(AUCTION_ID)).willReturn(Optional.of(highestBid));

		// when
		concludeProcessor.processConclusion(AUCTION_ID);

		// then
		await()
			.atMost(5, SECONDS)
			.untilAsserted(() -> {
				then(auctionReader).should(times(1)).getAuction(AUCTION_ID);
				then(bidReader).should(times(1)).findHighestBid(AUCTION_ID);
				then(concludeStore).should(times(1)).concludeAuctionWithBid(auction, highestBid);
				then(concludeStore).should(never()).concludeAuctionWithNoBids(any());
			});
	}

	@Test
	@DisplayName("DB 예외 발생 시 재시도 후 성공한다")
	void processConclusion_SuccessAfterRetry() {
		// given
		given(auctionReader.getAuction(AUCTION_ID))
			.willThrow(new DataAccessResourceFailureException("DB connection failed"))
			.willReturn(auction);

		given(bidReader.findHighestBid(AUCTION_ID)).willReturn(Optional.of(highestBid));

		// when
		concludeProcessor.processConclusion(AUCTION_ID);

		// then
		await()
			.atMost(5, SECONDS)
			.untilAsserted(() -> {
				then(auctionReader).should(times(2)).getAuction(AUCTION_ID);
				then(bidReader).should(times(1)).findHighestBid(AUCTION_ID);
				then(concludeStore).should(times(1)).concludeAuctionWithBid(auction, highestBid);
				then(concludeStore).should(never()).concludeAuctionWithNoBids(any());
			});
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	@DisplayName("최대 재시도 횟수 실패 시 @Recover 메소드가 호출된다")
	void processConclusion_FailAfterMaxRetriesAndRecover(CapturedOutput output) {
		// given
		given(auctionReader.getAuction(AUCTION_ID))
			.willThrow(new DataAccessResourceFailureException("DB connection failed"));

		// when
		assertThatCode(() -> concludeProcessor.processConclusion(AUCTION_ID))
			.doesNotThrowAnyException();

		// then
		// Awaitility로 최대 5초 기다리면서 조건을 polling
		await()
			.atMost(5, SECONDS)
			.untilAsserted(() -> {
				then(auctionReader).should(times(3)).getAuction(AUCTION_ID);
				then(bidReader).shouldHaveNoInteractions();
				then(concludeStore).shouldHaveNoInteractions();
			});

		assertThat(output).contains("낙찰 처리를 최종 실패했습니다. auctionId: 1");
	}
}
