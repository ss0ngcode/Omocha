package org.omocha.domain.conclude.exception;

import org.omocha.domain.auction.exception.AuctionException;
import org.omocha.domain.common.code.ErrorCode;

public class ConcludeInfoSaveException extends AuctionException {
	public ConcludeInfoSaveException(Long auctionId, Long buyerId) {
		super(
			ErrorCode.CONCLUDE_INFO_SAVE_FAILURE,
			String.format("낙찰 정보 저장에 실패했습니다. auctionId: %d, buyerId: %d", auctionId, buyerId)
		);
	}
}
