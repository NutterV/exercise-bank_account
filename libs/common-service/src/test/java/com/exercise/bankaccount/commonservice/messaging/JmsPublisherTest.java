package com.exercise.bankaccount.commonservice.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.bankaccount.commonservice.config.MessagingProperties;
import com.exercise.bankaccount.commonservice.config.QueueType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

class JmsPublisherTest {
	@Test
	void shouldSerializePayloadAndSendToConfiguredQueue() throws Exception {
		RecordingJmsTemplate jmsTemplate = new RecordingJmsTemplate();
		MessagingProperties messagingProperties = new MessagingProperties(
				Map.of(QueueType.TRANSACTION, "transactions.queue", QueueType.AUDIT, "audit.queue"));
		ObjectMapper objectMapper = new ObjectMapper();
		TestPublisher publisher = new TestPublisher(jmsTemplate, messagingProperties, objectMapper, QueueType.AUDIT);

		publisher.publish(new TestPayload("abc"));

		assertThat(jmsTemplate.destination()).isEqualTo("audit.queue");
		assertThat(jmsTemplate.payload()).isEqualTo("{\"value\":\"abc\"}");
	}

	private record TestPayload(String value) {
	}

	private static final class TestPublisher extends JmsPublisher<TestPayload> {
		private TestPublisher(JmsTemplate jmsTemplate, MessagingProperties messagingProperties, ObjectMapper objectMapper,
				QueueType queueType) {
			super(jmsTemplate, messagingProperties, objectMapper, queueType);
		}
	}

	private static final class RecordingJmsTemplate extends JmsTemplate {
		private final AtomicReference<String> destination = new AtomicReference<>();
		private final AtomicReference<String> payload = new AtomicReference<>();

		@Override
		public void send(String destinationName, MessageCreator messageCreator) {
			destination.set(destinationName);
			try {
				Message message = messageCreator.createMessage(recordingSession());
				payload.set(((TextMessage) message).getText());
			} catch (Exception exception) {
				throw new IllegalStateException(exception);
			}
		}

		private Session recordingSession() {
			return (Session) Proxy.newProxyInstance(Session.class.getClassLoader(), new Class[] { Session.class },
					(proxy, method, args) -> {
						if ("createTextMessage".equals(method.getName())) {
							return textMessage(String.valueOf(args[0]));
						}
						throw new UnsupportedOperationException(method.getName());
					});
		}

		private TextMessage textMessage(String body) {
			return (TextMessage) Proxy.newProxyInstance(TextMessage.class.getClassLoader(), new Class[] { TextMessage.class },
					(proxy, method, args) -> switch (method.getName()) {
						case "getText" -> body;
						case "setText" -> null;
						default -> throw new UnsupportedOperationException(method.getName());
					});
		}

		private String destination() {
			return destination.get();
		}

		private String payload() {
			return payload.get();
		}
	}
}
