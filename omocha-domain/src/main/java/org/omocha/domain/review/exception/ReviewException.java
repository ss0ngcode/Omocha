package org.omocha.domain.review.exception;

import org.omocha.domain.common.code.ErrorCode;
import org.omocha.domain.common.exception.OmochaException;

public abstract class ReviewException extends OmochaException {

	public ReviewException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public ReviewException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message, cause);
	}
}
