package org.omocha.domain.common.exception;

import org.omocha.domain.common.code.ErrorCode;

import lombok.Getter;

@Getter
public abstract class OmochaException extends RuntimeException {
	private final ErrorCode errorCode;

	public OmochaException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public OmochaException(ErrorCode errorCode, String message, Exception cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}
}
