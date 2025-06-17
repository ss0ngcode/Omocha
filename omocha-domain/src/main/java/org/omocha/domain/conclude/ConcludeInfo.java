package org.omocha.domain.conclude;

public class ConcludeInfo {

	public record ConcludeResult(
		Long auctionId,
		Long buyerId
	) {
		public static ConcludeInfo.ConcludeResult toInfo(
			Long auctionId,
			Long buyerId
		) {
			return new ConcludeInfo.ConcludeResult(auctionId, buyerId);
		}

		public boolean isConcluded() {
			return buyerId != null;
		}
	}
}
