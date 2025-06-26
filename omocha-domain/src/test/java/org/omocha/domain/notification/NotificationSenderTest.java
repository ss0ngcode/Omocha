package org.omocha.domain.notification;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.anyString;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omocha.domain.auction.AuctionReader;
import org.omocha.domain.bid.BidReader;
import org.omocha.domain.notification.enums.EventName;
import org.omocha.domain.util.DatabaseCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class NotificationSenderTest {

	@SpringBootApplication
	@EnableRetry
	@EntityScan(basePackages = "org.omocha.domain")
	@Import(DatabaseCleaner.class)
	static class TestApplication {
	}

	@Autowired
	private DatabaseCleaner databaseCleaner;
	@Autowired
	private NotificationSender notificationSender;
	@MockBean
	private NotificationStore notificationStore;
	@MockBean
	private NotificationReader notificationReader;    // Spring Boot Test용 MockBean 주입
	@MockBean
	private AuctionReader auctionReader;            // Spring Boot Test용 MockBean 주입
	@MockBean
	private BidReader bidReader;                    // Spring Boot Test용 MockBean 주입

	private SseEmitter mockEmitter;
	private String eventId;
	private EventName eventName;
	private Long memberId;
	private String data;

	@BeforeEach
	void setUp() {
		mockEmitter = mock(SseEmitter.class);
		eventId = "test-event-1";
		eventName = EventName.CONNECT;
		memberId = 123L;
		data = "{\"message\":\"test data\"}";
	}

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("SSE 이벤트 전송이 첫 시도에 성공한다")
	void sendSseEvent_SuccessOnFirstAttempt() throws IOException {
		// given
		willDoNothing().given(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

		// when
		notificationSender.sendSseEvent(mockEmitter, eventId, eventName, memberId, data);

		// then
		then(mockEmitter).should(times(1)).send(any(SseEmitter.SseEventBuilder.class));
		then(notificationStore).should(never()).emitterDelete(anyLong(), anyString());
	}

	@Test
	@DisplayName("SSE 이벤트 전송이 첫 시도에 실패하고, 두 번째 시도에 성공한다")
	void sendSseEvent_SuccessAfterRetry() throws IOException {
		// given
		willThrow(new IOException())
			.willDoNothing()
			.given(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

		// when
		notificationSender.sendSseEvent(mockEmitter, eventId, eventName, memberId, data);

		// then
		then(mockEmitter).should(times(2)).send(any(SseEmitter.SseEventBuilder.class));
		then(notificationStore).should(never()).emitterDelete(anyLong(), anyString());
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	@DisplayName("최대 재시도 횟수 실패 시 @Recover 메소드가 호출된다")
	void sendSseEvent_FailAfterMaxRetriesAndRecover(CapturedOutput output) throws IOException {
		// given
		willThrow(new IOException("Simulated I/O error"))
			.given(mockEmitter)
			.send(any(SseEmitter.SseEventBuilder.class));

		// when
		assertThatCode(() -> notificationSender.sendSseEvent(mockEmitter, eventId, eventName, memberId, data))
			.doesNotThrowAnyException();

		// then
		then(mockEmitter).should(times(3)).send(any(SseEmitter.SseEventBuilder.class));
		then(notificationStore).should(times(1)).emitterDelete(memberId, eventId);
		assertThat(output).contains("SSE 이벤트 전송을 최종 실패했습니다. memberId: " + memberId);
	}
}
