package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.Transaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Greedy best-fit-decreasing planner retained for benchmark comparison.
 */
final class BestFitDecreasingSubmissionBatchPlanner implements SubmissionBatchPlanner {
	@Override
	public List<AuditBatch> plan(List<Transaction> transactions, BigDecimal maxBatchTotal) {
		List<MutableBatch> batches = new ArrayList<>();
		List<Transaction> sortedTransactions = transactions.stream()
				.sorted(Comparator.comparing(Transaction::absoluteAmount).reversed()).toList();

		for (Transaction transaction : sortedTransactions) {
			BigDecimal absoluteAmount = transaction.absoluteAmount();
			if (absoluteAmount.compareTo(maxBatchTotal) > 0) {
				throw new IllegalArgumentException("Transaction exceeds batch value limit: " + transaction.id());
			}

			placeIntoBestFittingBatch(batches, absoluteAmount, maxBatchTotal);
		}

		return batches.stream().map(batch -> new AuditBatch(batch.totalValue(), batch.transactionCount())).toList();
	}

	private void placeIntoBestFittingBatch(List<MutableBatch> batches, BigDecimal absoluteAmount, BigDecimal maxBatchTotal) {
		MutableBatch bestBatch = null;
		BigDecimal smallestRemainingCapacity = null;

		for (MutableBatch batch : batches) {
			if (!batch.canAccept(absoluteAmount, maxBatchTotal)) {
				continue;
			}

			BigDecimal remainingCapacity = batch.remainingCapacityAfterAdding(absoluteAmount, maxBatchTotal);
			if (smallestRemainingCapacity == null || remainingCapacity.compareTo(smallestRemainingCapacity) < 0) {
				bestBatch = batch;
				smallestRemainingCapacity = remainingCapacity;
			}
		}

		if (bestBatch == null) {
			batches.add(new MutableBatch(absoluteAmount));
			return;
		}

		bestBatch.add(absoluteAmount);
	}

	private static final class MutableBatch {
		private BigDecimal totalValue;
		private int transactionCount;

		private MutableBatch(BigDecimal firstAmount) {
			this.totalValue = firstAmount;
			this.transactionCount = 1;
		}

		private boolean canAccept(BigDecimal absoluteAmount, BigDecimal maxBatchTotal) {
			return totalValue.add(absoluteAmount).compareTo(maxBatchTotal) <= 0;
		}

		private BigDecimal remainingCapacityAfterAdding(BigDecimal absoluteAmount, BigDecimal maxBatchTotal) {
			return maxBatchTotal.subtract(totalValue.add(absoluteAmount));
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
