package org.omocha.domain.auction.exception;

import org.omocha.domain.common.code.ErrorCode;
import org.omocha.domain.common.exception.OmochaException;

public abstract class AuctionException extends OmochaException {

	public AuctionException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public AuctionException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message, cause);
	}
}
