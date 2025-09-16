package org.omocha.domain.conclude.exception;

import org.omocha.domain.auction.exception.AuctionException;
import org.omocha.domain.common.code.ErrorCode;

public class ConcludeAuctionUpdateException extends AuctionException {
	public ConcludeAuctionUpdateException(Long auctionId, Long version) {
		super(
			ErrorCode.CONCLUDE_AUCTION_UPDATE_FAILURE,
			String.format("경매 상태 업데이트 중 충돌이 발생했습니다. auctionId: %d, version: %d", auctionId, version)
		);
	}
}
