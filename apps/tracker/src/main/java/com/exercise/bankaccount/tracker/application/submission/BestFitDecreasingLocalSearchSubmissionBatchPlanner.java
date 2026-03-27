package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.Transaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Best-fit-decreasing planner with a cheap local-search pass that tries to
 * eliminate whole batches by relocating their items.
 */
final class BestFitDecreasingLocalSearchSubmissionBatchPlanner implements SubmissionBatchPlanner {
	@Override
	public List<AuditBatch> plan(List<Transaction> transactions, BigDecimal maxBatchTotal) {
		List<BigDecimal> amounts = transactions.stream().map(transaction -> validateAndExtract(transaction, maxBatchTotal))
				.sorted(Comparator.reverseOrder()).toList();
		List<MutableBatch> batches = seedWithBestFitDecreasing(amounts, maxBatchTotal);
		boolean improved = true;

		while (improved) {
			improved = false;
			List<MutableBatch> candidates = batches.stream().sorted(Comparator
					.comparingInt(MutableBatch::transactionCount).thenComparing(MutableBatch::totalValue)).toList();
			for (MutableBatch candidate : candidates) {
				if (tryEliminateBatch(batches, candidate, maxBatchTotal)) {
					improved = true;
					break;
				}
			}
		}

		return batches.stream().map(batch -> new AuditBatch(batch.totalValue(), batch.transactionCount())).toList();
	}

	private List<MutableBatch> seedWithBestFitDecreasing(List<BigDecimal> amounts, BigDecimal maxBatchTotal) {
		List<MutableBatch> batches = new ArrayList<>();

		for (BigDecimal amount : amounts) {
			MutableBatch bestBatch = null;
			BigDecimal smallestRemainingCapacity = null;

			for (MutableBatch batch : batches) {
				if (!batch.canAccept(amount, maxBatchTotal)) {
					continue;
				}

				BigDecimal remainingCapacity = batch.remainingCapacityAfterAdding(amount, maxBatchTotal);
				if (smallestRemainingCapacity == null || remainingCapacity.compareTo(smallestRemainingCapacity) < 0) {
					bestBatch = batch;
					smallestRemainingCapacity = remainingCapacity;
				}
			}

			if (bestBatch == null) {
				batches.add(new MutableBatch(amount));
				continue;
			}

			bestBatch.add(amount);
		}

		return batches;
	}

	private boolean tryEliminateBatch(List<MutableBatch> batches, MutableBatch batchToEliminate, BigDecimal maxBatchTotal) {
		List<BigDecimal> amountsToRelocate = batchToEliminate.amountsDescending();
		List<Move> appliedMoves = new ArrayList<>(amountsToRelocate.size());

		for (BigDecimal amount : amountsToRelocate) {
			MutableBatch targetBatch = bestRelocationTarget(batches, batchToEliminate, amount, maxBatchTotal);
			if (targetBatch == null) {
				rollbackMoves(batchToEliminate, appliedMoves);
				return false;
			}

			batchToEliminate.remove(amount);
			targetBatch.add(amount);
			appliedMoves.add(new Move(targetBatch, amount));
		}

		batches.remove(batchToEliminate);
		return true;
	}

	private MutableBatch bestRelocationTarget(List<MutableBatch> batches, MutableBatch excludedBatch, BigDecimal amount,
			BigDecimal maxBatchTotal) {
		MutableBatch bestBatch = null;
		BigDecimal smallestRemainingCapacity = null;

		for (MutableBatch batch : batches) {
			if (batch == excludedBatch || !batch.canAccept(amount, maxBatchTotal)) {
				continue;
			}

			BigDecimal remainingCapacity = batch.remainingCapacityAfterAdding(amount, maxBatchTotal);
			if (smallestRemainingCapacity == null || remainingCapacity.compareTo(smallestRemainingCapacity) < 0) {
				bestBatch = batch;
				smallestRemainingCapacity = remainingCapacity;
			}
		}

		return bestBatch;
	}

	private void rollbackMoves(MutableBatch originalBatch, List<Move> appliedMoves) {
		for (int index = appliedMoves.size() - 1; index >= 0; index--) {
			Move move = appliedMoves.get(index);
			move.targetBatch().remove(move.amount());
			originalBatch.add(move.amount());
		}
	}

	private BigDecimal validateAndExtract(Transaction transaction, BigDecimal maxBatchTotal) {
		BigDecimal absoluteAmount = transaction.absoluteAmount();
		if (absoluteAmount.compareTo(maxBatchTotal) > 0) {
			throw new IllegalArgumentException("Transaction exceeds batch value limit: " + transaction.id());
		}
		return absoluteAmount;
	}

	private record Move(MutableBatch targetBatch, BigDecimal amount) {
	}

	private static final class MutableBatch {
		private final List<BigDecimal> amounts = new ArrayList<>();
		private BigDecimal totalValue;

		private MutableBatch(BigDecimal firstAmount) {
			this.amounts.add(firstAmount);
			this.totalValue = firstAmount;
		}

		private boolean canAccept(BigDecimal absoluteAmount, BigDecimal maxBatchTotal) {
			return totalValue.add(absoluteAmount).compareTo(maxBatchTotal) <= 0;
		}

		private BigDecimal remainingCapacityAfterAdding(BigDecimal absoluteAmount, BigDecimal maxBatchTotal) {
			return maxBatchTotal.subtract(totalValue.add(absoluteAmount));
		}

		private void add(BigDecimal absoluteAmount) {
			amounts.add(absoluteAmount);
			totalValue = totalValue.add(absoluteAmount);
		}

		private void remove(BigDecimal absoluteAmount) {
			amounts.remove(absoluteAmount);
			totalValue = totalValue.subtract(absoluteAmount);
		}

		private List<BigDecimal> amountsDescending() {
			return amounts.stream().sorted(Comparator.reverseOrder()).toList();
		}

		private BigDecimal totalValue() {
			return totalValue;
		}

		private int transactionCount() {
			return amounts.size();
		}
	}
}
