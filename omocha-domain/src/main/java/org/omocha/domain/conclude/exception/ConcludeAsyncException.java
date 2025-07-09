package org.omocha.domain.conclude.exception;

import org.omocha.domain.auction.exception.AuctionException;
import org.omocha.domain.common.code.ErrorCode;

public class ConcludeAsyncException extends AuctionException {
	public ConcludeAsyncException(Long auctionId, Throwable cause) {
		super(
			ErrorCode.CONCLUDE_ASYNC,
			"비동기 낙찰 처리중 오류가 발생했습니다. auctionId: " + auctionId,
			cause
		);
	}
}
