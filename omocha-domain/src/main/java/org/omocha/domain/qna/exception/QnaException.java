package org.omocha.domain.qna.exception;

import org.omocha.domain.common.code.ErrorCode;
import org.omocha.domain.common.exception.OmochaException;

public abstract class QnaException extends OmochaException {

	public QnaException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public QnaException(ErrorCode errorCode, String message, Exception cause) {
		super(errorCode, message, cause);
	}
}
