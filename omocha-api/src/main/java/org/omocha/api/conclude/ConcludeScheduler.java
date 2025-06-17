package org.omocha.api.conclude;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConcludeScheduler {

	private final ConcludeFacade concludeFacade;

	@Scheduled(cron = "0 * * * * *")
	public void scheduleAuctionConclusions() {
		concludeFacade.concludeAuctions();
	}
}
