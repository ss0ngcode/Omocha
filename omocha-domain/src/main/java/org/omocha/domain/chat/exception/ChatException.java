package org.omocha.domain.chat.exception;

import org.omocha.domain.common.code.ErrorCode;
import org.omocha.domain.common.exception.OmochaException;

public abstract class ChatException extends OmochaException {

	public ChatException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public ChatException(ErrorCode errorCode, String message, Exception cause) {
		super(errorCode, message, cause);
	}
}
