package org.omocha.api.conclude;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class ConcludeScheduler {

	private final ConcludeFacade concludeFacade;

	@Scheduled(cron = "0 * * * * *")
	public void scheduleAuctionConclusions() {
		concludeFacade.concludeAuctions();
	}
}
