package com.exercise.bankaccount.tracker.application.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exercise.bankaccount.common.model.AuditSubmission;
import com.exercise.bankaccount.common.model.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SubmissionBatcherTest {
	private static final BigDecimal TEST_MAX_BATCH_TOTAL = BigDecimal.valueOf(1_000_000);
	private static final SubmissionBatchPlanner FFD_PLANNER = new FirstFitDecreasingSubmissionBatchPlanner();

	@Test
	void shouldKeepTransactionsInOneBatchWhenAbsoluteTotalFitsTheLimit() {
		SubmissionBatcher submissionBatcher = new SubmissionBatcher(TEST_MAX_BATCH_TOTAL, auditSubmission -> {
		}, Executors.newSingleThreadExecutor());
		try {
			AuditSubmission submission = submissionBatcher
					.buildSubmission(List.of(transaction("600000"), transaction("-400000")));

			assertEquals(1, submission.batches().size());
			assertEquals(new BigDecimal("1000000"), submission.batches().get(0).totalValueOfAllTransactions());
			assertEquals(2, submission.batches().get(0).countOfTransactions());
		} finally {
			submissionBatcher.shutdown();
		}
	}

	@Test
	void shouldUseAbsoluteAmountsInsteadOfOffsettingCreditsAndDebits() {
		SubmissionBatcher submissionBatcher = new SubmissionBatcher(TEST_MAX_BATCH_TOTAL, auditSubmission -> {
		}, Executors.newSingleThreadExecutor());
		try {
			AuditSubmission submission = submissionBatcher
					.buildSubmission(List.of(transaction("600000"), transaction("-500000")));

			assertEquals(2, submission.batches().size());
			assertEquals(new BigDecimal("600000"), submission.batches().get(0).totalValueOfAllTransactions());
			assertEquals(new BigDecimal("500000"), submission.batches().get(1).totalValueOfAllTransactions());
		} finally {
			submissionBatcher.shutdown();
		}
	}

	@Test
	void shouldPackTransactionsToReduceBatchCount() {
		SubmissionBatcher submissionBatcher = new SubmissionBatcher(TEST_MAX_BATCH_TOTAL, auditSubmission -> {
		}, Executors.newSingleThreadExecutor());
		try {
			AuditSubmission submission = submissionBatcher.buildSubmission(List.of(transaction("600000"),
					transaction("400000"), transaction("600000"), transaction("400000")));

			assertEquals(2, submission.batches().size());
			assertEquals(new BigDecimal("1000000"), submission.batches().get(0).totalValueOfAllTransactions());
			assertEquals(2, submission.batches().get(0).countOfTransactions());
			assertEquals(new BigDecimal("1000000"), submission.batches().get(1).totalValueOfAllTransactions());
			assertEquals(2, submission.batches().get(1).countOfTransactions());
		} finally {
			submissionBatcher.shutdown();
		}
	}

	@Test
	void shouldUseFirstFitDecreasingOrderingBeforePacking() {
		SubmissionBatcher submissionBatcher = new SubmissionBatcher(TEST_MAX_BATCH_TOTAL, FFD_PLANNER,
				auditSubmission -> {
				}, Executors.newSingleThreadExecutor());
		try {
			AuditSubmission submission = submissionBatcher.buildSubmission(List.of(transaction("400000"),
					transaction("400000"), transaction("600000"), transaction("600000")));

			assertEquals(2, submission.batches().size());
			assertEquals(new BigDecimal("1000000"), submission.batches().get(0).totalValueOfAllTransactions());
			assertEquals(new BigDecimal("1000000"), submission.batches().get(1).totalValueOfAllTransactions());
		} finally {
			submissionBatcher.shutdown();
		}
	}

	@Test
	void shouldQueueEntireSubmissionWindowAndProcessItOnWorkerThread() throws Exception {
		CountDownLatch processed = new CountDownLatch(1);
		CopyOnWriteArrayList<AuditSubmission> processedSubmissions = new CopyOnWriteArrayList<>();
		ExecutorService workerExecutor = Executors.newSingleThreadExecutor();
		SubmissionBatcher submissionBatcher = new SubmissionBatcher(TEST_MAX_BATCH_TOTAL, auditSubmission -> {
			processedSubmissions.add(auditSubmission);
			processed.countDown();
		}, workerExecutor);
		try {
			submissionBatcher.processSubmission(List.of(transaction("600000"), transaction("400000"),
					transaction("600000"), transaction("400000")));

			assertTrue(processed.await(2, TimeUnit.SECONDS));
			assertEquals(0, submissionBatcher.pendingSubmissionCount());
			assertEquals(1, processedSubmissions.size());
			assertEquals(2, processedSubmissions.get(0).batches().size());
		} finally {
			submissionBatcher.shutdown();
		}
	}

	@Test
	void shouldPublishCompletedSubmissionAfterBatching() throws Exception {
		CountDownLatch processed = new CountDownLatch(1);
		CopyOnWriteArrayList<AuditSubmission> processedSubmissions = new CopyOnWriteArrayList<>();
		SubmissionBatcher submissionBatcher = new SubmissionBatcher(TEST_MAX_BATCH_TOTAL, auditSubmission -> {
			processedSubmissions.add(auditSubmission);
			processed.countDown();
		}, Executors.newSingleThreadExecutor());
		try {
			submissionBatcher.processSubmission(List.of(transaction("600000"), transaction("400000")));

			assertTrue(processed.await(2, TimeUnit.SECONDS));
			assertEquals(1, processedSubmissions.size());
			assertEquals(new BigDecimal("1000000"),
					processedSubmissions.get(0).batches().get(0).totalValueOfAllTransactions());
		} finally {
			submissionBatcher.shutdown();
		}
	}

	@Test
	void shouldContinueProcessingQueuedSubmissionsAfterPublisherFailure() throws Exception {
		CountDownLatch processed = new CountDownLatch(1);
		CopyOnWriteArrayList<AuditSubmission> processedSubmissions = new CopyOnWriteArrayList<>();
		java.util.concurrent.atomic.AtomicBoolean failFirst = new java.util.concurrent.atomic.AtomicBoolean(true);
		SubmissionBatcher submissionBatcher = new SubmissionBatcher(TEST_MAX_BATCH_TOTAL, auditSubmission -> {
			if (failFirst.compareAndSet(true, false)) {
				throw new IllegalStateException("boom");
			}
			processedSubmissions.add(auditSubmission);
			processed.countDown();
		}, Executors.newSingleThreadExecutor());
		try {
			submissionBatcher.processSubmission(List.of(transaction("600000"), transaction("400000")));
			submissionBatcher.processSubmission(List.of(transaction("500000"), transaction("300000")));

			assertTrue(processed.await(2, TimeUnit.SECONDS));
			assertEquals(1, processedSubmissions.size());
			assertEquals(new BigDecimal("800000"),
					processedSubmissions.get(0).batches().get(0).totalValueOfAllTransactions());
		} finally {
			submissionBatcher.shutdown();
		}
	}

	@Test
	void shouldRejectTransactionsThatExceedTheBatchLimitOnTheirOwn() {
		SubmissionBatcher submissionBatcher = new SubmissionBatcher(TEST_MAX_BATCH_TOTAL, auditSubmission -> {
		}, Executors.newSingleThreadExecutor());
		try {
			assertThrows(IllegalArgumentException.class,
					() -> submissionBatcher.buildSubmission(List.of(transaction("1000000.01"))));
		} finally {
			submissionBatcher.shutdown();
		}
	}

	@Test
	void shouldUseConfiguredBatchLimit() {
		SubmissionBatcher submissionBatcher = new SubmissionBatcher(BigDecimal.valueOf(700_000), auditSubmission -> {
		}, Executors.newSingleThreadExecutor());
		try {
			AuditSubmission submission = submissionBatcher
					.buildSubmission(List.of(transaction("400000"), transaction("300000"), transaction("300000")));

			assertEquals(2, submission.batches().size());
			assertEquals(new BigDecimal("700000"), submission.batches().get(0).totalValueOfAllTransactions());
		} finally {
			submissionBatcher.shutdown();
		}
	}

	private static Transaction transaction(String amount) {
		return new Transaction(UUID.randomUUID(), new BigDecimal(amount));
	}
}
