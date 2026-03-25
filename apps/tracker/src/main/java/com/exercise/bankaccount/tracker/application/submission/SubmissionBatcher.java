package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.AuditSubmission;
import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.tracker.application.config.TrackerSubmissionProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Queues sealed submission windows and batches them on a dedicated worker
 * thread.
 */
@Component
public class SubmissionBatcher implements SubmissionProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionBatcher.class);

	private final BlockingDeque<List<Transaction>> pendingSubmissions = new LinkedBlockingDeque<>();
	private final Consumer<AuditSubmission> auditSubmissionConsumer;
	private final ExecutorService workerExecutor;
	private final BigDecimal maxBatchTotal;

	/**
	 * Creates the production batcher with one background batching worker.
	 *
	 * @param trackerSubmissionProperties
	 *            configured submission batching settings
	 * @param batchedSubmissionPublisher
	 *            publisher for completed audit submissions
	 */
	@Autowired
	public SubmissionBatcher(TrackerSubmissionProperties trackerSubmissionProperties,
			BatchedSubmissionPublisher batchedSubmissionPublisher) {
		this(trackerSubmissionProperties.maxBatchTotal(),
				auditSubmission -> publishSubmission(batchedSubmissionPublisher, auditSubmission),
				Executors.newSingleThreadExecutor());
	}

	/**
	 * Test-friendly constructor that allows the worker executor and final consumer
	 * to be controlled explicitly.
	 *
	 * @param maxBatchTotal
	 *            configured maximum absolute total allowed per batch
	 * @param auditSubmissionConsumer
	 *            consumer invoked after a queued submission has been batched
	 * @param workerExecutor
	 *            executor running the batching loop
	 */
	SubmissionBatcher(BigDecimal maxBatchTotal, Consumer<AuditSubmission> auditSubmissionConsumer,
			ExecutorService workerExecutor) {
		this.maxBatchTotal = maxBatchTotal;
		this.auditSubmissionConsumer = auditSubmissionConsumer;
		this.workerExecutor = workerExecutor;
		this.workerExecutor.execute(this::runBatchingLoop);
	}

	/**
	 * Adds one sealed submission window to the back of the batching queue.
	 *
	 * @param transactions
	 *            completed submission window ready for background batching
	 */
	@Override
	public void processSubmission(List<Transaction> transactions) {
		pendingSubmissions.addLast(List.copyOf(transactions));
	}

	/**
	 * Stops the batching worker during bean shutdown.
	 */
	@PreDestroy
	public void shutdown() {
		workerExecutor.shutdownNow();
	}

	int pendingSubmissionCount() {
		return pendingSubmissions.size();
	}

	AuditSubmission buildSubmission(List<Transaction> transactions) {
		List<MutableBatch> batches = new ArrayList<>();
		List<Transaction> sortedTransactions = transactions.stream()
				.sorted(Comparator.comparing(Transaction::absoluteAmount).reversed()).toList();

		for (Transaction transaction : sortedTransactions) {
			BigDecimal absoluteAmount = transaction.absoluteAmount();
			if (absoluteAmount.compareTo(maxBatchTotal) > 0) {
				throw new IllegalArgumentException("Transaction exceeds batch value limit: " + transaction.id());
			}

			placeIntoBatch(batches, absoluteAmount);
		}

		return new AuditSubmission(
				batches.stream().map(batch -> new AuditBatch(batch.totalValue(), batch.transactionCount())).toList());
	}

	private void runBatchingLoop() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					List<Transaction> transactions = pendingSubmissions.takeFirst();
					auditSubmissionConsumer.accept(buildSubmission(transactions));
				} catch (RuntimeException exception) {
					LOGGER.error("Failed to batch or publish queued submission", exception);
				}
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}

	private static void publishSubmission(BatchedSubmissionPublisher batchedSubmissionPublisher,
			AuditSubmission auditSubmission) {
		try {
			batchedSubmissionPublisher.publish(auditSubmission);
			LOGGER.info("Processed queued submission into {} audit batches", auditSubmission.batches().size());
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to publish batched audit submission", exception);
		}
	}

	private void placeIntoBatch(List<MutableBatch> batches, BigDecimal absoluteAmount) {
		for (MutableBatch batch : batches) {
			if (batch.canAccept(absoluteAmount)) {
				batch.add(absoluteAmount);
				return;
			}
		}

		MutableBatch batch = new MutableBatch();
		batch.add(absoluteAmount);
		batches.add(batch);
	}

	private final class MutableBatch {
		private BigDecimal totalValue = BigDecimal.ZERO;
		private int transactionCount;

		private boolean canAccept(BigDecimal absoluteAmount) {
			return totalValue.add(absoluteAmount).compareTo(maxBatchTotal) <= 0;
		}

		private void add(BigDecimal absoluteAmount) {
			totalValue = totalValue.add(absoluteAmount);
			transactionCount++;
		}

		private BigDecimal totalValue() {
			return totalValue;
		}

		private int transactionCount() {
			return transactionCount;
		}
	}
}
