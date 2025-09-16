package org.omocha.api.conclude;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.omocha.api.config.TestConfig;
import org.omocha.domain.auction.Auction;
import org.omocha.domain.auction.AuctionReader;
import org.omocha.domain.auction.vo.Price;
import org.omocha.domain.bid.Bid;
import org.omocha.domain.bid.BidCommand;
import org.omocha.domain.bid.BidService;
import org.omocha.domain.category.Category;
import org.omocha.domain.conclude.ConcludeInfo;
import org.omocha.domain.conclude.ConcludeProcessor;
import org.omocha.domain.conclude.ConcludeReader;
import org.omocha.domain.member.Member;
import org.omocha.infra.auction.repository.AuctionRepository;
import org.omocha.infra.bid.repository.BidRepository;
import org.omocha.infra.category.repository.CategoryRepository;
import org.omocha.infra.member.repository.MemberRepository;
import org.omocha.util.DatabaseCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class ConcludeConcurrencyTest {

	@Autowired
	private DatabaseCleaner databaseCleaner;
	@Autowired
	private BidService bidService;
	@Autowired
	private BidRepository bidRepository;
	@Autowired
	private ConcludeProcessor concludeProcessor;
	@Autowired
	private AuctionRepository auctionRepository;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private ConcludeReader concludeReader;
	@Autowired
	private AuctionReader auctionReader;

	private Member seller;
	private Member bidder;
	private Member buyer;
	private Category category;
	private Long auctionId;

	@BeforeEach
	void setUp() {
		// 데이터베이스 초기화
		databaseCleaner.execute();

		// 테스트 멤버 생성
		seller = memberRepository.save(Member.builder().nickname("seller").build());
		bidder = memberRepository.save(Member.builder().nickname("bidder").build());

		// 카테고리 및 경매 데이터 생성
		category = categoryRepository.save(Category.builder().name("테스트").build());
		Auction auction = auctionRepository.save(Auction.builder()
			.memberId(seller.getMemberId())
			.title("동시성 테스트 경매")
			.startPrice(new Price(1000L))
			.nowPrice(new Price(1500L))
			.instantBuyPrice(new Price(2000L))
			.bidCount(1L)
			.endDate(LocalDateTime.now().minusMinutes(1))
			.category(category)
			.build());
		auctionId = auction.getAuctionId();

		// 입찰 데이터 생성
		bidRepository.save(Bid.builder()
			.auction(auction)
			.buyer(bidder)
			.bidPrice(new Price(1500L))
			.build());
	}

	// @AfterEach
	// void tearDown() {
	// 	databaseCleaner.execute();
	// }

	@Test
	@DisplayName("낙관락 동시 점유시 Retry 동작 확인")
	void retryMechanism_test() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(5);
		List<Callable<ConcludeInfo.ConcludeResult>> tasks = new ArrayList<>();

		for (int i = 0; i < 5; i++) {
			tasks.add(() -> {
				try {
					return concludeProcessor.processConclusion(auctionId);
				} catch (Exception e) {
					System.out.println("예외 발생: " + e.getClass().getSimpleName() + " - " + e.getMessage());
					return null;
				}
			});
		}

		// 동시 실행
		List<ConcludeInfo.ConcludeResult> results = executorService.invokeAll(tasks)
			.stream()
			.map(future -> {
				try {
					return future.get(30, TimeUnit.SECONDS);
				} catch (Exception e) {
					System.out.println("Future 처리 중 예외: " + e.getClass().getSimpleName());
					return null;
				}
			}).toList();

		// 결과 검증
		long successCount = results.stream().filter(result -> result != null).count();
		assertThat(successCount).isGreaterThan(0);

		// 최종 상태 확인
		Auction finalAuction = auctionReader.getAuction(auctionId);
		assertThat(finalAuction.getAuctionStatus()).isIn(
			Auction.AuctionStatus.CONCLUDED,
			Auction.AuctionStatus.NO_BIDS
		);

		System.out.println("성공한 처리 수: " + successCount + "/" + results.size());
		System.out.println("최종 경매 상태: " + finalAuction.getAuctionStatus());
	}

	@Test
	@DisplayName("즉시구매와 낙찰로직 동시성 테스트")
	void optimisticLock_concurrency_test() throws Exception {
		buyer = memberRepository.save(Member.builder().nickname("buyer").build());

		// 동시 실행 준비
		ExecutorService executorService = Executors.newFixedThreadPool(2);

		// 작업 정의 (리턴값을 받기 위해 Callable 사용)
		// 작업 1: 즉시 구매 (JPA)
		Callable<String> instantBuyTask = () -> {
			try {
				bidService.buyNow(new BidCommand.BuyNow(buyer.getMemberId(), auctionId));
				return "SUCCESS"; // 성공 시 문자열 리턴
			} catch (Exception e) {
				// 이 태스크가 실패하면 예외 이름을 리턴
				return e.getClass().getSimpleName();
			}
		};

		// 작업 2: 스케줄러 낙찰 (JdbcTemplate)
		Callable<String> schedulerConcludeTask = () -> {
			try {
				// @Recover가 예외를 처리하고 null을 리턴하므로, 그 결과를 문자열로 변환
				ConcludeInfo.ConcludeResult result = concludeProcessor.processConclusion(auctionId);
				return result == null ? "RECOVERED_NULL" : "SUCCESS";
			} catch (Exception e) {
				return e.getClass().getSimpleName();
			}
		};

		// 동시 실행 및 결과 취합 (진짜 동시성 테스트)
		List<String> results = executorService.invokeAll(List.of(instantBuyTask, schedulerConcludeTask))
			.stream()
			.map(future -> {
				try {
					return future.get(10, TimeUnit.SECONDS);
				} catch (Exception e) {
					return e.getClass().getSimpleName();
				}
			}).toList();

		// 결과 검증
		// 동시성 환경에서는 다음 중 하나의 시나리오가 발생:
		// 시나리오 1: 둘 다 SUCCESS (한쪽은 실제 처리, 다른 쪽은 멱등성 체크로 조기 리턴)
		// 시나리오 2: 하나는 SUCCESS, 다른 하나는 예외 또는 RECOVERED_NULL
		assertThat(results).contains("SUCCESS");

		// 최소 하나는 성공해야 함
		long successCount = results.stream().filter(result -> result.equals("SUCCESS")).count();
		assertThat(successCount).isGreaterThan(0);

		System.out.println("동시성 테스트 결과: " + results);

		// DB 최종 상태 검증 - 동시성 환경에서는 version이 1 또는 2가 될 수 있음
		Auction finalAuction = auctionReader.getAuction(auctionId);
		assertThat(finalAuction.getVersion()).isGreaterThan(0L); // 최소 1번은 업데이트됨
		assertThat(finalAuction.getAuctionStatus()).isEqualTo(Auction.AuctionStatus.CONCLUDED);

		System.out.println("동시성 테스트 결과 - 최종 version: " + finalAuction.getVersion());

		assertDoesNotThrow(() -> {
			concludeReader.getConclude(auctionId);
		});
	}
}
