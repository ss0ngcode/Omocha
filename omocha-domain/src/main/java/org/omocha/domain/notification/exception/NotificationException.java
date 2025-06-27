package org.omocha.domain.notification.exception;

import org.omocha.domain.common.code.ErrorCode;
import org.omocha.domain.common.exception.OmochaException;

public abstract class NotificationException extends OmochaException {

	public NotificationException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public NotificationException(ErrorCode errorCode, String message, Exception cause) {
		super(errorCode, message, cause);
	}
}
