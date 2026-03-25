package com.exercise.bankaccount.audit;

import com.exercise.bankaccount.common.model.AuditSubmission;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.*;
import java.util.concurrent.CountDownLatch;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects to Artemis and prints each received audit submission in the mocked
 * output format.
 */
final class AuditSubmissionListener implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuditSubmissionListener.class);

	private final ObjectMapper objectMapper;
	private final CountDownLatch shutdownLatch = new CountDownLatch(1);

	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private MessageConsumer consumer;

	AuditSubmissionListener() {
		this.objectMapper = new ObjectMapper().findAndRegisterModules();
	}

	void start(AuditRuntimeProperties properties) throws Exception {
		connectionFactory = ActiveMQJMSClient.createConnectionFactory(properties.brokerUrl(),
				"audit-connection-factory");
		connection = connectionFactory.createConnection();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		Queue queue = session.createQueue(properties.auditQueue());
		consumer = session.createConsumer(queue);
		consumer.setMessageListener(onMessage());
		connection.start();

		LOGGER.info("Mocked audit listening on {} via {}", properties.auditQueue(), properties.brokerUrl());
	}

	void awaitShutdown() throws InterruptedException {
		shutdownLatch.await();
	}

	@Override
	public void close() {
		closeQuietly(consumer);
		closeQuietly(session);
		closeQuietly(connection);
		closeQuietly(connectionFactory);
		shutdownLatch.countDown();
	}

	private MessageListener onMessage() {
		return message -> {
			try {
				System.out.println(AuditConsoleFormatter.format(readSubmission(message)));
			} catch (Exception exception) {
				LOGGER.error("Failed to process audit submission message", exception);
			}
		};
	}

	private AuditSubmission readSubmission(Message message) throws Exception {
		if (message instanceof TextMessage textMessage) {
			return objectMapper.readValue(textMessage.getText(), AuditSubmission.class);
		}

		throw new JMSException("Unsupported message type: " + message.getClass().getName());
	}

	private void closeQuietly(AutoCloseable closeable) {
		if (closeable == null) {
			return;
		}

		try {
			closeable.close();
		} catch (Exception exception) {
			LOGGER.warn("Failed to close audit resource cleanly", exception);
		}
	}
}
