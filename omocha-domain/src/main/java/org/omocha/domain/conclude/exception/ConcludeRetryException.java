package org.omocha.domain.conclude.exception;

import org.omocha.domain.auction.exception.AuctionException;
import org.omocha.domain.common.code.ErrorCode;

public class ConcludeRetryException extends AuctionException {
	public ConcludeRetryException(Long auctionId) {
		super(
			ErrorCode.CONCLUDE_RETRY,
			"낙찰 재시도 횟수를 초과하였습니다. auctionId: " + auctionId
		);
	}
}
