package org.omocha.domain.notification;

import java.io.IOException;

import org.omocha.domain.notification.enums.EventName;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSenderImpl implements NotificationSender {

	private final SseRetryExecutor retryExecutor;

	@Override
	public void sendSseEvent(
		SseEmitter emitter,
		String eventId,
		EventName eventName,
		Long memberId,
		String data
	) {
		try {
			retryExecutor.sendWithRetry(emitter, eventId, eventName, memberId, data);
		} catch (IOException e) {
			// TODO: 자체 Exception으로 처리하기
			throw new RuntimeException("SSE 이벤트 전송에 실패했습니다. memberId: " + memberId, e);
		}
	}

	// SseNotificationSenderImpl 안에서만 사용하는 static 내부 클래스
	@Component
	@RequiredArgsConstructor
	public static class SseRetryExecutor {

		private final NotificationStore notificationStore;

		@Retryable(
			retryFor = {
				IOException.class
			},
			maxAttempts = 3,
			backoff = @Backoff(delay = 200)
		)
		public void sendWithRetry(
			SseEmitter emitter, String eventId, EventName eventName, Long memberId, String data
		) throws IOException {
			if (emitter != null) {
				emitter.send(SseEmitter.event()
					.name(eventName.toString())
					.id(eventId)
					.data(data, MediaType.APPLICATION_JSON)
					.reconnectTime(1000L));
			}
		}

		@Recover
		public void recover(
			IOException e,
			SseEmitter emitter,
			String eventId,
			EventName eventName,
			Long memberId,
			String data
		) {
			log.error("SSE 이벤트 전송을 최종 실패했습니다. memberId: {}", memberId, e);
			notificationStore.emitterDelete(memberId, eventId);
		}
	}
}