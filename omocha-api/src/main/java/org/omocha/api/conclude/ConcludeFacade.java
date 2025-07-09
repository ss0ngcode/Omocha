package org.omocha.api.conclude;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.omocha.domain.auction.ExpiredAuctionFetcher;
import org.omocha.domain.conclude.ConcludeInfo;
import org.omocha.domain.conclude.exception.ConcludeAsyncException;
import org.omocha.domain.conclude.exception.ConcludeRetryException;
import org.omocha.domain.notification.NotificationService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcludeFacade {

	private final ExpiredAuctionFetcher expiredAuctionFetcher;
	private final ConcludeAsyncService concludeAsyncService;
	private final NotificationService notificationService;

	public void concludeAuctions() {

		List<Long> expiredAuctionIds = expiredAuctionFetcher.fetchExpiredAuctionIds();

		if (expiredAuctionIds.isEmpty()) {
			log.info("처리할 만료된 경매가 없습니다.");
			return;
		}

		final int BATCH_SIZE = 100;
		for (int i = 0; i < expiredAuctionIds.size(); i += BATCH_SIZE) {
			List<Long> batchList = expiredAuctionIds.subList(i, Math.min(i + BATCH_SIZE, expiredAuctionIds.size()));

			List<CompletableFuture<Void>> futures = batchList.stream()
				.map(this::executeAuctionConclusion)
				.toList();

			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		}
	}

	private CompletableFuture<Void> executeAuctionConclusion(Long auctionId) {
		CompletableFuture<ConcludeInfo.ConcludeResult> futureResult =
			concludeAsyncService.processConclusionAsync(auctionId);

		return futureResult
			.handle((concludeResult, throwable) -> {
				// 비동기 처리중 예외가 발생한 경우
				if (throwable != null) {
					throw new ConcludeAsyncException(auctionId, throwable);
				}

				// 낙찰 재시도 횟수를 초과한 경우
				if (concludeResult == null) {
					throw new ConcludeRetryException(auctionId);
				}

				return concludeResult;
			})
			.thenAccept(result -> {
				if (!result.isConcluded()) {
					notificationService.sendNoBidEvent(result.auctionId());
					return;
				}
				notificationService.sendConcludeEvent(result.auctionId());
			})
			.exceptionally(error -> {
				log.error("[Auction ID: {}] 처리 최종 실패: {}", auctionId, error.getMessage());
				return null;
			});
	}
}
