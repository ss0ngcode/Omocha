package org.omocha.domain.notification.exception;

import org.omocha.domain.common.code.ErrorCode;

public class NotificationSendException extends NotificationException {
	public NotificationSendException(Long memberId) {
		super(
			ErrorCode.NOTIFICATION_SEND,
			"SSE 이벤트 전송에 실패했습니다. memberId: " + memberId
		);
	}

	public NotificationSendException(Long memberId, Exception cause) {
		super(
			ErrorCode.NOTIFICATION_SEND,
			"SSE 이벤트 전송에 실패했습니다. memberId: " + memberId,
			cause
		);
	}
}
