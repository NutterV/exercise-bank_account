package com.exercise.bankaccount.tracker.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.AuditSubmission;
import com.exercise.bankaccount.common.model.BalanceResponse;
import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.producer.ProducerApplication;
import com.exercise.bankaccount.producer.application.TransactionPublisher;
import com.exercise.bankaccount.tracker.TrackerApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.RestTemplate;

class ProducerTrackerEndToEndTest {
	private static final int BROKER_PORT = randomPort();
	private static final String TRANSACTION_QUEUE = "producer-tracker.e2e.transactions." + UUID.randomUUID();
	private static final String AUDIT_QUEUE = "producer-tracker.e2e.audit." + UUID.randomUUID();
	private static final EmbeddedActiveMQ BROKER = startBroker();

	private ConfigurableApplicationContext producerContext;
	private ConfigurableApplicationContext trackerContext;
	private TransactionPublisher transactionPublisher;
	private JmsTemplate auditQueueTemplate;
	private ObjectMapper objectMapper;
	private int trackerPort;

	@BeforeEach
	void startApplications() {
		trackerContext = startTracker();
		producerContext = startProducer();

		transactionPublisher = producerContext.getBean(TransactionPublisher.class);
		objectMapper = trackerContext.getBean(ObjectMapper.class);
		auditQueueTemplate = new JmsTemplate(trackerContext.getBean(ConnectionFactory.class));
		auditQueueTemplate.setReceiveTimeout(200L);
		trackerPort = ((WebServerApplicationContext) trackerContext).getWebServer().getPort();
	}

	@AfterEach
	void stopApplications() {
		if (producerContext != null) {
			producerContext.close();
		}
		if (trackerContext != null) {
			trackerContext.close();
		}
	}

	@AfterAll
	static void stopBroker() throws Exception {
		BROKER.stop();
	}

	@Test
	void shouldPublishExactAuditPayloadsForControlledProducerTransactions() throws Exception {
		List<Transaction> transactions = List.of(
				new Transaction(UUID.fromString("00000000-0000-0000-0000-000000000001"), new BigDecimal("7")),
				new Transaction(UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("-3")),
				new Transaction(UUID.fromString("00000000-0000-0000-0000-000000000003"), new BigDecimal("6")),
				new Transaction(UUID.fromString("00000000-0000-0000-0000-000000000004"), new BigDecimal("-4")),
				new Transaction(UUID.fromString("00000000-0000-0000-0000-000000000005"), new BigDecimal("6")),
				new Transaction(UUID.fromString("00000000-0000-0000-0000-000000000006"), new BigDecimal("-4")),
				new Transaction(UUID.fromString("00000000-0000-0000-0000-000000000007"), new BigDecimal("4")),
				new Transaction(UUID.fromString("00000000-0000-0000-0000-000000000008"), new BigDecimal("-4")));

		for (Transaction transaction : transactions) {
			transactionPublisher.publish(transaction);
		}

		List<AuditSubmission> auditSubmissions = awaitAuditSubmissions(2, Duration.ofSeconds(10));
		BalanceResponse balanceResponse = awaitBalance(8.0d, Duration.ofSeconds(10));

		assertEquals(List.of(
				new AuditSubmission(List.of(new AuditBatch(new BigDecimal("10"), 2),
						new AuditBatch(new BigDecimal("10"), 2))),
				new AuditSubmission(List.of(new AuditBatch(new BigDecimal("10"), 2),
						new AuditBatch(new BigDecimal("8"), 2)))),
				auditSubmissions);
		assertNotNull(balanceResponse);
		assertEquals(8.0d, balanceResponse.balance());
	}

	private BalanceResponse awaitBalance(double expectedBalance, Duration timeout) throws InterruptedException {
		RestTemplate restTemplate = new RestTemplate();
		long deadline = System.nanoTime() + timeout.toNanos();

		while (System.nanoTime() < deadline) {
			BalanceResponse response = restTemplate
					.getForObject("http://localhost:" + trackerPort + "/balances/current", BalanceResponse.class);
			if (response != null && response.balance() == expectedBalance) {
				return response;
			}
			Thread.sleep(25L);
		}

		return restTemplate.getForObject("http://localhost:" + trackerPort + "/balances/current", BalanceResponse.class);
	}

	private List<AuditSubmission> awaitAuditSubmissions(int expectedCount, Duration timeout) throws Exception {
		List<AuditSubmission> submissions = new ArrayList<>();
		long deadline = System.nanoTime() + timeout.toNanos();

		while (submissions.size() < expectedCount && System.nanoTime() < deadline) {
			String payload = (String) auditQueueTemplate.receiveAndConvert(AUDIT_QUEUE);
			if (payload == null) {
				Thread.sleep(25L);
				continue;
			}
			submissions.add(objectMapper.readValue(payload, AuditSubmission.class));
		}

		return submissions;
	}

	private ConfigurableApplicationContext startProducer() {
		return new SpringApplicationBuilder(ProducerApplication.class)
				.run(commonMessagingProperties("bank-account.producer.lifecycle.enabled=false",
						"spring.main.web-application-type=none"));
	}

	private ConfigurableApplicationContext startTracker() {
		return new SpringApplicationBuilder(TrackerApplication.class)
				.run(commonMessagingProperties("server.port=0",
						"bank-account.tracker.submission.submission-size=4",
						"bank-account.tracker.submission.initial-buffer-count=2",
						"bank-account.tracker.submission.max-buffer-count=10",
						"bank-account.tracker.submission.max-batch-total=10"));
	}

	private String[] commonMessagingProperties(String... additionalProperties) {
		List<String> properties = new ArrayList<>(List.of("--spring.artemis.mode=native",
				"--spring.artemis.embedded.enabled=false",
				"--spring.artemis.broker-url=tcp://127.0.0.1:" + BROKER_PORT,
				"--bank-account.messaging.queues.transaction=" + TRANSACTION_QUEUE,
				"--bank-account.messaging.queues.audit=" + AUDIT_QUEUE,
				"--logging.level.org.apache.activemq.audit=ERROR",
				"--logging.level.org.apache.activemq.artemis=WARN"));
		for (String property : additionalProperties) {
			properties.add("--" + property);
		}
		return properties.toArray(String[]::new);
	}

	private static EmbeddedActiveMQ startBroker() {
		try {
			Configuration configuration = new ConfigurationImpl().setPersistenceEnabled(false)
					.setSecurityEnabled(false).setJMXManagementEnabled(false);
			configuration.addAcceptorConfiguration("tcp", "tcp://127.0.0.1:" + BROKER_PORT);

			EmbeddedActiveMQ broker = new EmbeddedActiveMQ();
			broker.setConfiguration(configuration);
			broker.start();
			return broker;
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to start embedded Artemis broker for tests", exception);
		}
	}

	private static int randomPort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to reserve random TCP port", exception);
		}
	}
}
