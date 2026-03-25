package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.Transaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Branch-and-bound planner that searches for the minimum possible batch count.
 */
final class ExactSubmissionBatchPlanner implements SubmissionBatchPlanner {
	@Override
	public List<AuditBatch> plan(List<Transaction> transactions, BigDecimal maxBatchTotal) {
		List<BigDecimal> amounts = transactions.stream().map(transaction -> validateAndExtract(transaction, maxBatchTotal))
				.sorted(Comparator.reverseOrder()).toList();
		List<BigDecimal> suffixTotals = suffixTotals(amounts);
		List<MutableBatch> incumbent = seedIncumbent(amounts, maxBatchTotal);
		SearchState searchState = new SearchState(copyOf(incumbent));

		search(amounts, suffixTotals, 0, new ArrayList<>(), searchState, maxBatchTotal);
		return toAuditBatches(searchState.bestSolution());
	}

	private void search(List<BigDecimal> amounts, List<BigDecimal> suffixTotals, int index, List<MutableBatch> current,
			SearchState searchState, BigDecimal maxBatchTotal) {
		if (index == amounts.size()) {
			searchState.captureIfBetter(current);
			return;
		}
		if (!canBeatIncumbent(current, suffixTotals.get(index), searchState.bestCount(), maxBatchTotal)) {
			return;
		}

		BigDecimal amount = amounts.get(index);
		List<BigDecimal> triedCapacities = new ArrayList<>();
		int existingBatchCount = current.size();
		for (int batchIndex = 0; batchIndex < existingBatchCount; batchIndex++) {
			MutableBatch batch = current.get(batchIndex);
			if (!batch.canAccept(amount, maxBatchTotal) || containsEquivalentCapacity(triedCapacities, batch.remainingCapacity(maxBatchTotal))) {
				continue;
			}
			triedCapacities.add(batch.remainingCapacity(maxBatchTotal));
			batch.add(amount);
			search(amounts, suffixTotals, index + 1, current, searchState, maxBatchTotal);
			batch.remove(amount);
		}

		if (current.size() + 1 >= searchState.bestCount()) {
			return;
		}
		current.add(new MutableBatch(amount));
		search(amounts, suffixTotals, index + 1, current, searchState, maxBatchTotal);
		current.removeLast();
	}

	private boolean canBeatIncumbent(List<MutableBatch> current, BigDecimal remainingTotal, int incumbentCount,
			BigDecimal maxBatchTotal) {
		if (current.size() >= incumbentCount) {
			return false;
		}

		BigDecimal remainingCapacity = current.stream().map(batch -> batch.remainingCapacity(maxBatchTotal))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal uncoveredTotal = remainingTotal.subtract(remainingCapacity).max(BigDecimal.ZERO);
		int minimumAdditionalBatches = uncoveredTotal.signum() == 0 ? 0
				: uncoveredTotal.divide(maxBatchTotal, 0, RoundingMode.CEILING).intValueExact();
		return current.size() + minimumAdditionalBatches < incumbentCount;
	}

	private boolean containsEquivalentCapacity(List<BigDecimal> capacities, BigDecimal candidate) {
		return capacities.stream().anyMatch(capacity -> capacity.compareTo(candidate) == 0);
	}

	private List<BigDecimal> suffixTotals(List<BigDecimal> amounts) {
		List<BigDecimal> suffixTotals = new ArrayList<>(amounts.size() + 1);
		for (int index = 0; index <= amounts.size(); index++) {
			suffixTotals.add(BigDecimal.ZERO);
		}
		for (int index = amounts.size() - 1; index >= 0; index--) {
			suffixTotals.set(index, suffixTotals.get(index + 1).add(amounts.get(index)));
		}
		return suffixTotals;
	}

	private List<MutableBatch> seedIncumbent(List<BigDecimal> amounts, BigDecimal maxBatchTotal) {
		List<MutableBatch> batches = new ArrayList<>();
		for (BigDecimal amount : amounts) {
			boolean placed = false;
			for (MutableBatch batch : batches) {
				if (batch.canAccept(amount, maxBatchTotal)) {
					batch.add(amount);
					placed = true;
					break;
				}
			}
			if (!placed) {
				batches.add(new MutableBatch(amount));
			}
		}
		return batches;
	}

	private List<MutableBatch> copyOf(List<MutableBatch> batches) {
		List<MutableBatch> copy = new ArrayList<>(batches.size());
		for (MutableBatch batch : batches) {
			copy.add(batch.copy());
		}
		return copy;
	}

	private List<AuditBatch> toAuditBatches(List<MutableBatch> batches) {
		return batches.stream().map(batch -> new AuditBatch(batch.totalValue(), batch.transactionCount())).toList();
	}

	private BigDecimal validateAndExtract(Transaction transaction, BigDecimal maxBatchTotal) {
		BigDecimal absoluteAmount = transaction.absoluteAmount();
		if (absoluteAmount.compareTo(maxBatchTotal) > 0) {
			throw new IllegalArgumentException("Transaction exceeds batch value limit: " + transaction.id());
		}
		return absoluteAmount;
	}

	private static final class SearchState {
		private List<MutableBatch> bestSolution;

		private SearchState(List<MutableBatch> bestSolution) {
			this.bestSolution = bestSolution;
		}

		private int bestCount() {
			return bestSolution.size();
		}

		private List<MutableBatch> bestSolution() {
			return bestSolution;
		}

		private void captureIfBetter(List<MutableBatch> candidate) {
			if (candidate.size() < bestSolution.size()) {
				bestSolution = candidate.stream().map(MutableBatch::copy).toList();
			}
		}
	}

	private static final class MutableBatch {
		private BigDecimal totalValue;
		private int transactionCount;

		private MutableBatch(BigDecimal firstAmount) {
			this.totalValue = firstAmount;
			this.transactionCount = 1;
		}

		private MutableBatch(BigDecimal totalValue, int transactionCount) {
			this.totalValue = totalValue;
			this.transactionCount = transactionCount;
		}

		private boolean canAccept(BigDecimal amount, BigDecimal maxBatchTotal) {
			return totalValue.add(amount).compareTo(maxBatchTotal) <= 0;
		}

		private BigDecimal remainingCapacity(BigDecimal maxBatchTotal) {
			return maxBatchTotal.subtract(totalValue);
		}

		private void add(BigDecimal amount) {
			totalValue = totalValue.add(amount);
			transactionCount++;
		}

		private void remove(BigDecimal amount) {
			totalValue = totalValue.subtract(amount);
			transactionCount--;
		}

		private BigDecimal totalValue() {
			return totalValue;
		}

		private int transactionCount() {
			return transactionCount;
		}

		private MutableBatch copy() {
			return new MutableBatch(totalValue, transactionCount);
		}
	}
}
