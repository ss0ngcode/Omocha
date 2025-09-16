package org.omocha.api.conclude;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.omocha.api.config.TestConfig;
import org.omocha.util.DatabaseCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StopWatch;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(properties = {
	"logging.level.org.hibernate.SQL=OFF",
	"logging.level.org.hibernate.type.descriptor.sql=OFF",
	"logging.level.p6spy=OFF",
	"spring.jpa.show-sql=false"
})
class ConcludePerformanceTest {

	@Autowired
	private DatabaseCleaner databaseCleaner;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private ConcludeFacade concludeFacade;

	private static final int MEMBER_COUNT = 10000;
	private static final int AUCTION_COUNT = 200000;
	private static final int BIDS_PER_AUCTION = 5;
	private static final int TIME_OUT_SECONDS = 60;

	private static final int CONCLUDED_COUNT = (int)(AUCTION_COUNT * 0.7);  // 70%는 이미 종료된 경매
	private static final int NO_BIDS_COUNT = (int)(AUCTION_COUNT * 0.15); // 15%는 유찰된 경매
	private static final int ONGOING_BIDDING_COUNT = (int)(AUCTION_COUNT * 0.05); // 5%는 아직 진행중인 경매, 10%가 실제 낙찰 대상

	@BeforeEach
	void setupLargeData() {
		// 데이터베이스 초기화
		databaseCleaner.execute();

		// 더미데이터 생성 시작
		System.out.println("JdbcTemplate으로 대량 테스트 데이터 생성을 시작합니다...");
		StopWatch setupStopWatch = new StopWatch();

		// 더미 카테고리 생성
		setupStopWatch.start("더미 카테고리 생성");
		jdbcTemplate.update("INSERT INTO category (name, order_index) VALUES (?, ?)", "테스트", 0);
		Long categoryId = jdbcTemplate.queryForObject("SELECT category_id FROM category WHERE name = '테스트'",
			Long.class);
		setupStopWatch.stop();

		// Member 데이터 생성
		setupStopWatch.start("Member " + MEMBER_COUNT + "건 삽입");
		String memberSql =
			"INSERT INTO member (email, password, nickname, username, birth, phone_number, average_rating, role, member_status, email_verified, created_at, updated_at) "
				+
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		jdbcTemplate.batchUpdate(memberSql, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setString(1, "test" + i + "@email.com");
				ps.setString(2, "password123");
				ps.setString(3, "nickname" + i);
				ps.setString(4, "username" + i);
				ps.setObject(5, LocalDate.now().minusYears(20));
				ps.setString(6, "010-1234-" + String.format("%04d", i));
				ps.setDouble(7, 0.0);
				ps.setString(8, "ROLE_USER");
				ps.setString(9, "ACTIVATE");
				ps.setBoolean(10, true);
				ps.setObject(11, LocalDateTime.now());
				ps.setObject(12, LocalDateTime.now());
			}

			@Override
			public int getBatchSize() {
				return MEMBER_COUNT;
			}
		});
		setupStopWatch.stop();

		// 생성된 memberId 목록 조회
		setupStopWatch.start("Member ID 목록 조회");
		List<Long> memberIds = jdbcTemplate.queryForList("SELECT member_id FROM member", Long.class);
		setupStopWatch.stop();

		// Auction 데이터 생성
		setupStopWatch.start("Auction " + AUCTION_COUNT + "건 삽입");
		String auctionSql =
			"INSERT INTO auction (member_id, title, content, start_price, now_price, bid_count, bid_unit, like_count, auction_status, start_date, end_date, category_id, created_at, updated_at, version) "
				+
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		jdbcTemplate.batchUpdate(auctionSql, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, memberIds.get(i % MEMBER_COUNT));
				ps.setString(2, "부하 테스트 경매 " + i);
				ps.setString(3, "경매 내용 " + i);
				ps.setLong(4, 1000L);
				ps.setLong(5, 1000L);
				ps.setLong(6, 0L);
				ps.setLong(7, 100L);
				ps.setLong(8, 0L);
				ps.setObject(10, LocalDateTime.now().minusDays(5)); // 시작일은 과거로 통일
				ps.setLong(12, categoryId);
				ps.setObject(13, LocalDateTime.now());
				ps.setObject(14, LocalDateTime.now());
				ps.setObject(15, 0);

				// === 상태 및 종료 시간 분기 로직 ===
				String status;
				LocalDateTime endDate;

				if (i < CONCLUDED_COUNT) {
					// 그룹 1 (70%): 이미 낙찰된 경매 (과거에 종료)
					status = "CONCLUDED";
					endDate = LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextLong(2, 10));
				} else if (i < CONCLUDED_COUNT + NO_BIDS_COUNT) {
					// 그룹 2 (15%): 이미 유찰된 경매 (과거에 종료)
					status = "NO_BIDS";
					endDate = LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextLong(2, 10));
				} else if (i < CONCLUDED_COUNT + NO_BIDS_COUNT + ONGOING_BIDDING_COUNT) {
					// 그룹 3 (5%): 아직 진행 중인 경매 (미래에 종료)
					status = "BIDDING";
					endDate = LocalDateTime.now().plusMinutes(ThreadLocalRandom.current().nextLong(1, 30));
				} else {
					// 그룹 4 (10%): 낙찰 로직의 주 대상이 될 경매 (최근 1시간 내 종료)
					status = "BIDDING";
					endDate = LocalDateTime.now().minusMinutes(ThreadLocalRandom.current().nextLong(1, 60));
				}

				ps.setString(9, status);
				ps.setObject(11, endDate);
			}

			@Override
			public int getBatchSize() {
				return AUCTION_COUNT;
			}
		});

		setupStopWatch.stop();

		// 생성된 auctionId 목록 조회
		setupStopWatch.start("입찰이 필요한 Auction ID 목록 조회");
		List<Long> auctionIdsForBidding = jdbcTemplate.queryForList(
			"SELECT auction_id FROM auction WHERE auction_status != 'NO_BIDS'", // NO_BIDS가 아닌 경매만 선택
			Long.class
		);
		setupStopWatch.stop();

		// Bid 데이터 생성
		setupStopWatch.start("Bid " + (auctionIdsForBidding.size() * BIDS_PER_AUCTION) + "건 삽입");
		String bidSql = "INSERT INTO bid (auction_id, buyer_member_id, bid_price, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
		List<Object[]> bidArgs = new ArrayList<>();

		// 전체 auctionIds 대신 입찰이 필요한 auctionIdsForBidding 사용
		for (Long auctionId : auctionIdsForBidding) {
			for (int j = 0; j < BIDS_PER_AUCTION; j++) {
				long bidderId = memberIds.get((auctionId.intValue() + j + 1) % MEMBER_COUNT);
				long price = 1000L + (100L * (j + 1));
				bidArgs.add(new Object[] {auctionId, bidderId, price, LocalDateTime.now(), LocalDateTime.now()});
			}
		}
		jdbcTemplate.batchUpdate(bidSql, bidArgs);
		setupStopWatch.stop();

		System.out.println("✓ 데이터 준비 완료!");
		System.out.println(setupStopWatch.prettyPrint());
	}

	// @AfterEach
	// void tearDown() {
	// 	databaseCleaner.execute();
	// }

	@Test
	@DisplayName("대량 데이터 기반 낙찰 로직 성능 단일 테스트 (1분 제한)")
	void runAuctionConclusionLoadSingleTest() {
		StopWatch testStopWatch = new StopWatch("낙찰 로직 전체 실행 시간");

		testStopWatch.start();
		concludeFacade.concludeAuctions();
		testStopWatch.stop();

		System.out.println(testStopWatch.prettyPrint());
		double totalTimeSeconds = testStopWatch.getTotalTimeSeconds();
		System.out.println(">>> 최종 실행 시간: " + totalTimeSeconds + "초");

		assertTrue(totalTimeSeconds < TIME_OUT_SECONDS, "낙찰 로직이 1분 제한 시간("
			+ TIME_OUT_SECONDS + "초)을 초과했습니다. 총 " + totalTimeSeconds + "초 소요.");
	}

	@RepeatedTest(3)
	@DisplayName("대량 데이터 기반 낙찰 로직 성능 반복 테스트 [{currentRepetition}/{totalRepetitions}] (1분 제한)")
	void runAuctionConclusionLoadRepeatedTest(RepetitionInfo repetitionInfo) {
		System.out.println("====== " + repetitionInfo.getCurrentRepetition() + "번째 성능 테스트 시작 ======");
		StopWatch testStopWatch = new StopWatch("낙찰 로직 전체 실행 시간");

		testStopWatch.start();
		concludeFacade.concludeAuctions();
		testStopWatch.stop();

		System.out.println(testStopWatch.prettyPrint());
		double totalTimeSeconds = testStopWatch.getTotalTimeSeconds();
		System.out.println(">>> " + repetitionInfo.getCurrentRepetition() + "번째 최종 실행 시간: " + totalTimeSeconds + "초");
		System.out.println("========================================\n");

		assertTrue(totalTimeSeconds < TIME_OUT_SECONDS,
			"[" + repetitionInfo.getCurrentRepetition() + "번째 실행] 낙찰 로직이 1분 제한 시간("
				+ TIME_OUT_SECONDS + "초)을 초과했습니다. 총 " + totalTimeSeconds + "초 소요.");
	}
}
