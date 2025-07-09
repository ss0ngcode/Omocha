package org.omocha.domain.member.exception;

import org.omocha.domain.common.code.ErrorCode;
import org.omocha.domain.common.exception.OmochaException;

public abstract class MemberException extends OmochaException {

	public MemberException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public MemberException(ErrorCode errorCode, String message, Throwable cause) {
		super(errorCode, message, cause);
	}
}
