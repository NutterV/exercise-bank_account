package com.exercise.bankaccount.tracker.application.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exercise.bankaccount.common.model.Transaction;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SubmissionBufferCoordinatorTest {
	private static final int TEST_SUBMISSION_SIZE = 1_000;

	@Test
	void shouldDispatchCompletedSubmissionWhenTheThousandthTransactionArrives() throws Exception {
		RecordingSubmissionProcessor processor = new RecordingSubmissionProcessor();
		SubmissionBufferCoordinator coordinator = new SubmissionBufferCoordinator(processor, TEST_SUBMISSION_SIZE, 2,
				10);

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
		SubmissionBufferCoordinator coordinator = new SubmissionBufferCoordinator(processor, TEST_SUBMISSION_SIZE, 2,
				10);

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
		SubmissionBufferCoordinator coordinator = new SubmissionBufferCoordinator(processor, TEST_SUBMISSION_SIZE, 2,
				10);
		CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
			List<Future<?>> futures = new java.util.ArrayList<>();
			for (int index = 0; index < threadCount; index++) {
				futures.add(executorService.submit(() -> {
					try {
						start.await();
						for (int count = 0; count < transactionsPerThread; count++) {
							coordinator.record(transaction(BigDecimal.ONE));
						}
					} catch (InterruptedException exception) {
						Thread.currentThread().interrupt();
						throw new IllegalStateException(exception);
					}
				}));
			}

			start.countDown();
			for (Future<?> future : futures) {
				future.get(15, TimeUnit.SECONDS);
			}
			executorService.shutdown();
			assertTrue(executorService.awaitTermination(15, TimeUnit.SECONDS));
		}

		assertTrue(processor.awaitSubmission());
		assertEquals(2, processor.submissions().size());
		assertEquals(2_000, processor.submissions().stream().mapToInt(List::size).sum());
		Set<UUID> transactionIds = new HashSet<>();
		for (List<Transaction> submission : processor.submissions()) {
			for (Transaction transaction : submission) {
				assertTrue(transactionIds.add(transaction.id()));
			}
		}
		assertEquals(2_000, transactionIds.size());
	}

	@Test
	void shouldCreateThirdBufferWhenBothInitialBuffersAreUnavailable() throws Exception {
		BlockingFirstSubmissionProcessor processor = new BlockingFirstSubmissionProcessor();
		SubmissionBufferCoordinator coordinator = new SubmissionBufferCoordinator(processor, 2, 2, 10);
		Thread firstWindowThread = new Thread(() -> {
			coordinator.record(transaction(BigDecimal.ONE));
			coordinator.record(transaction(BigDecimal.ONE));
		});

		firstWindowThread.start();
		assertTrue(processor.awaitFirstSubmissionStarted());

		coordinator.record(transaction(BigDecimal.ONE));
		coordinator.record(transaction(BigDecimal.ONE));

		assertEquals(3, bufferCount(coordinator));

		processor.releaseFirstSubmission();
		firstWindowThread.join(TimeUnit.SECONDS.toMillis(5));
		assertTrue(processor.awaitSecondSubmission());
		assertEquals(2, processor.submissions().size());
	}

	private static Transaction transaction(BigDecimal amount) {
		return new Transaction(UUID.randomUUID(), amount);
	}

	private static int bufferCount(SubmissionBufferCoordinator coordinator) throws ReflectiveOperationException {
		Field bufferCountField = SubmissionBufferCoordinator.class.getDeclaredField("bufferCount");
		bufferCountField.setAccessible(true);
		return ((java.util.concurrent.atomic.AtomicInteger) bufferCountField.get(coordinator)).get();
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
			return latch.await(15, TimeUnit.SECONDS);
		}

		private List<List<Transaction>> submissions() {
			return submissions;
		}
	}

	private static final class BlockingFirstSubmissionProcessor implements SubmissionProcessor {
		private final CountDownLatch firstSubmissionStarted = new CountDownLatch(1);
		private final CountDownLatch firstSubmissionRelease = new CountDownLatch(1);
		private final CountDownLatch secondSubmission = new CountDownLatch(1);
		private final List<List<Transaction>> submissions = new java.util.concurrent.CopyOnWriteArrayList<>();

		@Override
		public void processSubmission(List<Transaction> transactions) {
			submissions.add(transactions);
			if (submissions.size() == 1) {
				firstSubmissionStarted.countDown();
				try {
					firstSubmissionRelease.await(15, TimeUnit.SECONDS);
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(exception);
				}
				return;
			}
			secondSubmission.countDown();
		}

		private boolean awaitFirstSubmissionStarted() throws InterruptedException {
			return firstSubmissionStarted.await(15, TimeUnit.SECONDS);
		}

		private void releaseFirstSubmission() {
			firstSubmissionRelease.countDown();
		}

		private boolean awaitSecondSubmission() throws InterruptedException {
			return secondSubmission.await(15, TimeUnit.SECONDS);
		}

		private List<List<Transaction>> submissions() {
			return submissions;
		}
	}
}
