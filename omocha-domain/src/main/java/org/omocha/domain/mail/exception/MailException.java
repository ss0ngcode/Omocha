package org.omocha.domain.mail.exception;

import org.omocha.domain.common.code.ErrorCode;
import org.omocha.domain.common.exception.OmochaException;

public abstract class MailException extends OmochaException {

	public MailException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public MailException(ErrorCode errorCode, String message, Exception cause) {
		super(errorCode, message, cause);
	}
}
