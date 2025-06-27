package org.omocha.domain.image.exception;

import org.omocha.domain.common.code.ErrorCode;
import org.omocha.domain.common.exception.OmochaException;

public abstract class ImageException extends OmochaException {

	public ImageException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public ImageException(ErrorCode errorCode, String message, Exception cause) {
		super(errorCode, message, cause);
	}
}
