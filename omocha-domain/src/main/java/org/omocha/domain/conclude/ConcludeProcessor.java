package org.omocha.domain.conclude;

public interface ConcludeProcessor {

	ConcludeInfo.ConcludeResult processConclusion(Long expiredAuctionId);
}
