package org.omocha.api.conclude;

import java.util.concurrent.CompletableFuture;

import org.omocha.domain.conclude.ConcludeInfo;
import org.omocha.domain.conclude.ConcludeProcessor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConcludeAsyncService {

	private final ConcludeProcessor concludeProcessor;

	@Async("concludeAsyncExecutor")
	public CompletableFuture<ConcludeInfo.ConcludeResult> processConclusionAsync(Long auctionId) {
		try {
			return CompletableFuture.completedFuture(concludeProcessor.processConclusion(auctionId));
		} catch (Exception e) {
			return CompletableFuture.failedFuture(e);
		}
	}
}
