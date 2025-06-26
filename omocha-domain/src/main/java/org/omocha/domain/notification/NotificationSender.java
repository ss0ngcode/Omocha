package org.omocha.domain.notification;

import org.omocha.domain.notification.enums.EventName;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationSender {

	void sendSseEvent(
		SseEmitter emitter,
		String eventId,
		EventName eventName,
		Long memberId,
		String data
	);
}
