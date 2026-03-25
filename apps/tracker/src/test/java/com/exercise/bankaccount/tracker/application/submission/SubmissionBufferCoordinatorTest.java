package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.tracker.application.SubmissionProcessor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionBufferCoordinatorTest {

	private static final int TEST_SUBMISSION_SIZE = 1_000;

	@Test
	void shouldDispatchCompletedSubmissionWhenTheThousandthTransactionArrives() throws Exception {
		RecordingSubmissionProcessor processor = new RecordingSubmissionProcessor();
		SubmissionBufferCoordinator coordinator = new SubmissionBufferCoordinator(processor, TEST_SUBMISSION_SIZE, 2, 10);

		for (int count = 0; count < TEST_SUBMISSION_SIZE; count++) {
			coordinator.record(transaction(BigDecimal.ONE));
		}

		assertTrue(processor.awaitSubmission());
		assertEquals(1, processor.submissions().size());
		assertEquals(TEST_SUBMISSION_SIZE, processor.submissions().get(0).size());
	}

	@Test
	void shouldRecycleBuffersAcrossMultipleCompletedSubmissionWindows() throws Exception {
		RecordingSubmissionProcessor processor = new RecordingSubmissionProcessor(2);
		SubmissionBufferCoordinator coordinator = new SubmissionBufferCoordinator(processor, TEST_SUBMISSION_SIZE, 2, 10);

		for (int count = 0; count < (TEST_SUBMISSION_SIZE * 2) + 1; count++) {
			coordinator.record(transaction(BigDecimal.ONE));
		}

		assertTrue(processor.awaitSubmission());
		assertEquals(2, processor.submissions().size());
		assertEquals(TEST_SUBMISSION_SIZE, processor.submissions().get(0).size());
		assertEquals(TEST_SUBMISSION_SIZE, processor.submissions().get(1).size());
	}

	@Test
	void shouldHandleConcurrentRecordingWithoutLosingTransactions() throws Exception {
		int threadCount = 4;
		int transactionsPerThread = 500;
		RecordingSubmissionProcessor processor = new RecordingSubmissionProcessor(2);
		SubmissionBufferCoordinator coordinator = new SubmissionBufferCoordinator(processor, TEST_SUBMISSION_SIZE, 2, 10);
		CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
			for (int index = 0; index < threadCount; index++) {
				executorService.submit(() -> {
					try {
						start.await();
						for (int count = 0; count < transactionsPerThread; count++) {
							coordinator.record(transaction(BigDecimal.ONE));
						}
					} catch (InterruptedException exception) {
						Thread.currentThread().interrupt();
						throw new IllegalStateException(exception);
					}
				});
			}

			start.countDown();
			executorService.shutdown();
			assertTrue(executorService.awaitTermination(2, TimeUnit.SECONDS));
		}

		assertTrue(processor.awaitSubmission());
		assertEquals(2, processor.submissions().size());
		assertEquals(2_000, processor.submissions().stream().mapToInt(List::size).sum());
	}

	private static Transaction transaction(BigDecimal amount) {
		return new Transaction(UUID.randomUUID(), amount);
	}

	private static final class RecordingSubmissionProcessor implements SubmissionProcessor {

		private final CountDownLatch latch;
		private final List<List<Transaction>> submissions = new java.util.concurrent.CopyOnWriteArrayList<>();

		private RecordingSubmissionProcessor(int expectedSubmissions) {
			this.latch = new CountDownLatch(expectedSubmissions);
		}

		private RecordingSubmissionProcessor() {
			this(1);
		}

		@Override
		public void processSubmission(List<Transaction> transactions) {
			submissions.add(transactions);
			latch.countDown();
		}

		private boolean awaitSubmission() throws InterruptedException {
			return latch.await(2, TimeUnit.SECONDS);
		}

		private List<List<Transaction>> submissions() {
			return submissions;
		}
	}
}
