package org.omocha.domain.bid.exception;

import org.omocha.domain.common.code.ErrorCode;
import org.omocha.domain.common.exception.OmochaException;

public abstract class BidException extends OmochaException {

	public BidException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public BidException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message, cause);
	}
}
