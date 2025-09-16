package org.omocha.domain.conclude.exception;

import org.omocha.domain.auction.exception.AuctionException;
import org.omocha.domain.common.code.ErrorCode;

public class ConcludeAlreadyProcessedException extends AuctionException {
	public ConcludeAlreadyProcessedException(Long auctionId) {
		super(
			ErrorCode.CONCLUDE_ALREADY_PROCESSED,
			"이미 처리된 경매입니다. auctionId: " + auctionId
		);
	}
}
