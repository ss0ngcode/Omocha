package org.omocha.api.conclude;

import java.util.List;

import org.omocha.domain.auction.ExpiredAuctionFetcher;
import org.omocha.domain.conclude.ConcludeInfo;
import org.omocha.domain.conclude.ConcludeProcessor;
import org.omocha.domain.notification.NotificationService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConcludeFacade {

	private final ExpiredAuctionFetcher expiredAuctionFetcher;
	private final ConcludeProcessor concludeProcessor;
	private final NotificationService notificationService;

	public void concludeAuctions() {
		List<Long> expiredAuctionIds = expiredAuctionFetcher.fetchExpiredAuctionIds();

		for (Long expiredAuctionId : expiredAuctionIds) {
			ConcludeInfo.ConcludeResult concludeResult = concludeProcessor.processConclusion(expiredAuctionId);

			if (!concludeResult.isConcluded()) {
				// 입찰이 없어서 낙찰이 되지 않은 경우
				notificationService.sendNoBidEvent(concludeResult.auctionId());
				return;
			}

			// 입찰이 있어서 낙찰이 된 경우
			notificationService.sendConcludeEvent(concludeResult.auctionId());
		}
	}
}
