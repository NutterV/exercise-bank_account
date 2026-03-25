package com.exercise.bankaccount.producer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.producer.domain.TransactionFactory;
import com.exercise.bankaccount.producer.utils.SlidingWindowRateLimiter;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class TransactionProducerWorkerPerformanceTest {
	private static void awaitAndRun(CountDownLatch startLatch, TransactionProducerWorker worker) {
		try {
			startLatch.await();
			worker.run();
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
		}
	}

	@Test
	void shouldPublishAtMostTwentyFiveCreditAndDebitMessagesPerSecondForTwoSeconds() throws Exception {
		RecordingPublisher publisher = new RecordingPublisher();
		TransactionFactory transactionFactory = new TransactionFactory(BigDecimal.valueOf(200),
				BigDecimal.valueOf(500000));
		TransactionProducerWorker creditWorker = new TransactionProducerWorker("credit-test-worker",
				TransactionDirection.CREDIT, new SlidingWindowRateLimiter(25, 1_000L), transactionFactory, publisher);
		TransactionProducerWorker debitWorker = new TransactionProducerWorker("debit-test-worker",
				TransactionDirection.DEBIT, new SlidingWindowRateLimiter(25, 1_000L), transactionFactory, publisher);

		CountDownLatch startLatch = new CountDownLatch(1);
		try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
			executorService.submit(() -> awaitAndRun(startLatch, creditWorker));
			executorService.submit(() -> awaitAndRun(startLatch, debitWorker));

			long startedAt = System.currentTimeMillis();
			startLatch.countDown();
			Thread.sleep(2_050L);
			creditWorker.stop();
			debitWorker.stop();
			executorService.shutdownNow();
			assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS));

			assertEquals(25, publisher.countForBucket(startedAt, 0, true));
			assertEquals(25, publisher.countForBucket(startedAt, 0, false));
			assertEquals(25, publisher.countForBucket(startedAt, 1, true));
			assertEquals(25, publisher.countForBucket(startedAt, 1, false));
			assertTrue(publisher.maxCountInAnyBucket(startedAt, true) <= 25);
			assertTrue(publisher.maxCountInAnyBucket(startedAt, false) <= 25);
		}
	}

	private static final class RecordingPublisher extends TransactionPublisher {
		private final List<PublishedTransaction> publishedTransactions = new CopyOnWriteArrayList<>();

		private RecordingPublisher() {
			super(null, null, null);
		}

		@Override
		public void publish(Transaction transaction) {
			publishedTransactions
					.add(new PublishedTransaction(System.currentTimeMillis(), transaction.amount().signum() > 0));
		}

		int countForBucket(long startedAt, int bucketIndex, boolean credit) {
			return (int) publishedTransactions.stream()
					.filter(publishedTransaction -> publishedTransaction.credit == credit)
					.filter(publishedTransaction -> bucketFor(startedAt,
							publishedTransaction.publishedAt) == bucketIndex)
					.count();
		}

		int maxCountInAnyBucket(long startedAt, boolean credit) {
			return publishedTransactions.stream().filter(publishedTransaction -> publishedTransaction.credit == credit)
					.mapToInt(publishedTransaction -> countForBucket(startedAt,
							bucketFor(startedAt, publishedTransaction.publishedAt), credit))
					.max().orElse(0);
		}

		private int bucketFor(long startedAt, long publishedAt) {
			return (int) ((publishedAt - startedAt) / 1_000L);
		}
	}

	private record PublishedTransaction(long publishedAt, boolean credit) {
	}
}
