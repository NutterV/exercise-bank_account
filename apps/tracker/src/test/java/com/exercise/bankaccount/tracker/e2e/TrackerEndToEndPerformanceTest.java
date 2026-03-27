package com.exercise.bankaccount.tracker.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exercise.bankaccount.common.model.AuditSubmission;
import com.exercise.bankaccount.common.model.BalanceResponse;
import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.tracker.application.performance.PerformanceCaptureSnapshot;
import com.exercise.bankaccount.tracker.application.performance.SubmissionPerformanceSnapshot;
import com.exercise.bankaccount.tracker.application.performance.TrackerPerformanceCaptureService;
import com.exercise.bankaccount.tracker.TrackerApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TrackerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(Lifecycle.PER_CLASS)
class TrackerEndToEndPerformanceTest {
	private static final int BROKER_PORT = randomPort();
	private static final String TRANSACTION_QUEUE = "tracker.e2e.transactions." + UUID.randomUUID();
	private static final String AUDIT_QUEUE = "tracker.e2e.audit." + UUID.randomUUID();
	private static final EmbeddedActiveMQ BROKER = startBroker();

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TrackerPerformanceCaptureService trackerPerformanceCaptureService;

	@Autowired
	private TestRestTemplate restTemplate;

	@LocalServerPort
	private int port;

	private JmsTemplate jmsTemplate;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.artemis.mode", () -> "native");
		registry.add("spring.artemis.embedded.enabled", () -> "false");
		registry.add("spring.artemis.broker-url", () -> "tcp://127.0.0.1:" + BROKER_PORT);
		registry.add("bank-account.messaging.queues.transaction", () -> TRANSACTION_QUEUE);
		registry.add("bank-account.messaging.queues.audit", () -> AUDIT_QUEUE);
		registry.add("bank-account.tracker.submission.submission-size", () -> 1_000);
		registry.add("bank-account.tracker.submission.initial-buffer-count", () -> 2);
		registry.add("bank-account.tracker.submission.max-buffer-count", () -> 10);
		registry.add("bank-account.tracker.submission.max-batch-total", () -> "1000000");
		registry.add("bank-account.tracker.performance.enabled", () -> "true");
		registry.add("logging.level.org.apache.activemq.audit", () -> "ERROR");
		registry.add("logging.level.org.apache.activemq.artemis", () -> "WARN");
	}

	@BeforeEach
	void setUpTemplates() {
		jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setReceiveTimeout(200L);
		drainQueue(TRANSACTION_QUEUE);
		drainQueue(AUDIT_QUEUE);
	}

	@AfterAll
	static void stopBroker() throws Exception {
		BROKER.stop();
	}

	@ParameterizedTest(name = "should process {0} transactions end-to-end")
	@MethodSource("payloadSizes")
	void shouldProcessTransactionsEndToEndAndReportBenchmark(int transactionCount) throws Exception {
		List<Transaction> transactions = buildTransactions(transactionCount);
		BigDecimal expectedBalance = transactions.stream().map(Transaction::amount).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		List<ExpectedSubmission> expectedSubmissions = expectedSubmissions(transactions);
		Duration timeout = timeoutFor(transactionCount);
		startCapture(transactionCount, expectedSubmissions.size());

		long startedAt = System.nanoTime();
		for (Transaction transaction : transactions) {
			jmsTemplate.send(TRANSACTION_QUEUE, session -> {
				try {
					return session.createTextMessage(objectMapper.writeValueAsString(transaction));
				} catch (Exception exception) {
					throw new IllegalStateException("Failed to serialize test transaction", exception);
				}
			});
		}
		long publishedAt = System.nanoTime();

		PerformanceCaptureSnapshot captureSnapshot = awaitCaptureCompletion(timeout);
		PerformanceCaptureSnapshot stoppedCapture = stopCapture();
		List<AuditSubmission> actualSubmissions = awaitAuditSubmissions(expectedSubmissions.size(), timeout);
		BalanceResponse balanceResponse = restTemplate.getForObject("http://localhost:" + port + "/balances/current",
				BalanceResponse.class);

		assertEquals(expectedSubmissions.size(), actualSubmissions.size());
		for (int index = 0; index < expectedSubmissions.size(); index++) {
			ExpectedSubmission expectedSubmission = expectedSubmissions.get(index);
			AuditSubmission actualSubmission = actualSubmissions.get(index);

			assertEquals(1_000, actualSubmission.batches().stream().mapToInt(batch -> batch.countOfTransactions()).sum());
			assertEquals(expectedSubmission.absoluteTotal(),
					actualSubmission.batches().stream().map(batch -> batch.totalValueOfAllTransactions())
							.reduce(BigDecimal.ZERO, BigDecimal::add));
			assertTrue(actualSubmission.batches().stream()
					.allMatch(batch -> batch.totalValueOfAllTransactions().compareTo(new BigDecimal("1000000")) <= 0));
		}

		assertNotNull(balanceResponse);
		assertEquals(expectedBalance.doubleValue(), balanceResponse.balance());
		assertNull(jmsTemplate.receive(AUDIT_QUEUE), "Audit queue should be empty after consuming expected submissions");
		assertTrue(captureSnapshot.captureComplete(), "Tracker capture should reach all expected milestones");
		assertEquals(transactionCount, stoppedCapture.processedTransactionCount());
		assertEquals(expectedSubmissions.size(), stoppedCapture.publishedSubmissionCount());

		Duration publishDuration = Duration.ofNanos(publishedAt - startedAt);
		Duration fullIngestDuration = Duration.ofNanos(
				captureSnapshot.lastTransactionProcessedAtNanos() - captureSnapshot.firstTransactionConsumedAtNanos());
		Duration totalDuration = Duration
				.ofNanos(captureSnapshot.lastSubmissionPublishedAtNanos() - captureSnapshot.firstTransactionConsumedAtNanos());

		System.out.printf("Tracker E2E benchmark: %d transactions -> publish %d ms, full ingest %d ms, total %d ms%n",
				transactionCount, publishDuration.toMillis(), fullIngestDuration.toMillis(), totalDuration.toMillis());
		for (SubmissionPerformanceSnapshot submission : stoppedCapture.submissions()) {
			Duration packingDuration = durationBetween(submission.batchingStartedAtNanos(),
					submission.batchingCompletedAtNanos());
			Duration firstConsumedToPublishedDuration = durationBetween(submission.firstTransactionConsumedAtNanos(),
					submission.publishedAtNanos());
			System.out.printf(
					"  submission %d -> pack %d ms, first-consumed-to-published %d ms, batches %d%n",
					submission.submissionIndex(), packingDuration.toMillis(), firstConsumedToPublishedDuration.toMillis(),
					submission.publishedBatchCount());
		}
	}

	private void startCapture(int expectedTransactionCount, int expectedSubmissionCount) {
		trackerPerformanceCaptureService.startCapture(expectedTransactionCount, expectedSubmissionCount);
	}

	private PerformanceCaptureSnapshot awaitCaptureCompletion(Duration timeout) throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();

		while (System.nanoTime() < deadline) {
			PerformanceCaptureSnapshot snapshot = trackerPerformanceCaptureService.currentCapture();
			if (snapshot != null && snapshot.captureComplete()) {
				return snapshot;
			}
			Thread.sleep(25L);
		}

		return trackerPerformanceCaptureService.currentCapture();
	}

	private PerformanceCaptureSnapshot stopCapture() {
		return trackerPerformanceCaptureService.stopCapture();
	}

	private List<AuditSubmission> awaitAuditSubmissions(int expectedCount, Duration timeout) throws Exception {
		List<AuditSubmission> submissions = new ArrayList<>();
		long deadline = System.nanoTime() + timeout.toNanos();

		while (submissions.size() < expectedCount && System.nanoTime() < deadline) {
			String payload = (String) jmsTemplate.receiveAndConvert(AUDIT_QUEUE);
			if (payload == null) {
				Thread.sleep(25L);
				continue;
			}
			submissions.add(objectMapper.readValue(payload, AuditSubmission.class));
		}

		return submissions;
	}

	private Duration durationBetween(long startedAtNanos, long finishedAtNanos) {
		if (startedAtNanos == 0L || finishedAtNanos == 0L) {
			return Duration.ZERO;
		}
		return Duration.ofNanos(Math.max(0L, finishedAtNanos - startedAtNanos));
	}

	private void drainQueue(String queueName) {
		while (jmsTemplate.receive(queueName) != null) {
			// keep draining
		}
	}

	private static List<Transaction> buildTransactions(int transactionCount) {
		return IntStream.range(0, transactionCount).mapToObj(index -> new Transaction(UUID.randomUUID(), amountFor(index)))
				.toList();
	}

	private static BigDecimal amountFor(int index) {
		return switch (index % 4) {
		case 0 -> new BigDecimal("500000");
		case 1 -> new BigDecimal("-200000");
		case 2 -> new BigDecimal("300000");
		default -> new BigDecimal("-100000");
		};
	}

	private static List<ExpectedSubmission> expectedSubmissions(List<Transaction> transactions) {
		List<ExpectedSubmission> submissions = new ArrayList<>();

		for (int start = 0; start + 1_000 <= transactions.size(); start += 1_000) {
			List<Transaction> window = transactions.subList(start, start + 1_000);
			BigDecimal absoluteTotal = window.stream().map(Transaction::absoluteAmount).reduce(BigDecimal.ZERO,
					BigDecimal::add);
			submissions.add(new ExpectedSubmission(absoluteTotal));
		}

		return submissions;
	}

	private static Stream<Arguments> payloadSizes() {
		return Stream.of(Arguments.of(10_000), Arguments.of(25_000), Arguments.of(50_000), Arguments.of(75_000),
				Arguments.of(100_000));
	}

	private static Duration timeoutFor(int transactionCount) {
		if (transactionCount <= 10_000) {
			return Duration.ofSeconds(30);
		}
		if (transactionCount <= 25_000) {
			return Duration.ofSeconds(60);
		}
		if (transactionCount <= 50_000) {
			return Duration.ofSeconds(90);
		}
		if (transactionCount <= 75_000) {
			return Duration.ofSeconds(120);
		}
		return Duration.ofSeconds(180);
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

	private record ExpectedSubmission(BigDecimal absoluteTotal) {
	}
}
