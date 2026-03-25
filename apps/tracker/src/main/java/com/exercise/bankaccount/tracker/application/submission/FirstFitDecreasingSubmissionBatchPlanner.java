package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.Transaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Greedy first-fit-decreasing planner for audit batches.
 */
final class FirstFitDecreasingSubmissionBatchPlanner implements SubmissionBatchPlanner {
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

			placeIntoFirstFittingBatch(batches, absoluteAmount, maxBatchTotal);
		}

		return toAuditBatches(batches);
	}

	private void placeIntoFirstFittingBatch(List<MutableBatch> batches, BigDecimal absoluteAmount, BigDecimal maxBatchTotal) {
		for (MutableBatch batch : batches) {
			if (batch.canAccept(absoluteAmount, maxBatchTotal)) {
				batch.add(absoluteAmount);
				return;
			}
		}

		batches.add(new MutableBatch(absoluteAmount));
	}

	private List<AuditBatch> toAuditBatches(List<MutableBatch> batches) {
		return batches.stream().map(batch -> new AuditBatch(batch.totalValue(), batch.transactionCount())).toList();
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
