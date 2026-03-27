package com.exercise.bankaccount.tracker.application.submission;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.Transaction;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubmissionBatchPlannerPerformanceTest {
	private static final BigDecimal MAX_BATCH_TOTAL = BigDecimal.valueOf(1_000_000);
	private static final Duration EXACT_TIME_BUDGET = Duration.ofMillis(750);

	private final SubmissionBatchPlanner firstFitDecreasingPlanner = new FirstFitDecreasingSubmissionBatchPlanner();
	private final SubmissionBatchPlanner bestFitDecreasingPlanner = new BestFitDecreasingSubmissionBatchPlanner();
	private final SubmissionBatchPlanner bestFitDecreasingLocalSearchPlanner = new BestFitDecreasingLocalSearchSubmissionBatchPlanner();
	private final SubmissionBatchPlanner exactPlanner = new ExactSubmissionBatchPlanner();

	@Test
	void shouldBenchmarkPlannerTradeOffsAcrossIncreasingSubmissionSizes() {
		benchmarkScenario("tracker-e2e-like", List.of("500000", "-200000", "300000", "-100000"),
				List.of(10_000, 25_000, 50_000, 75_000, 100_000));
		benchmarkScenario("counterexample-rich", List.of("600000", "500000", "300000", "200000", "200000", "200000"),
				List.of(6, 12, 18, 24, 30, 36, 42, 48, 50, 60, 72, 75, 100, 150, 250, 500, 1_000));
	}

	private void benchmarkScenario(String scenarioName, List<String> repeatingPattern, List<Integer> sizes) {
		System.out.printf("Planner benchmark scenario: %s%n", scenarioName);
		boolean exactBudgetExceeded = false;

		for (int size : sizes) {
			List<Transaction> transactions = buildTransactions(repeatingPattern, size);

			BenchmarkResult ffdResult = benchmark(firstFitDecreasingPlanner, transactions);
			assertBatchesRespectLimit(ffdResult.batches());
			BenchmarkResult bfdResult = benchmark(bestFitDecreasingPlanner, transactions);
			assertBatchesRespectLimit(bfdResult.batches());
			BenchmarkResult bfdLocalSearchResult = benchmark(bestFitDecreasingLocalSearchPlanner, transactions);
			assertBatchesRespectLimit(bfdLocalSearchResult.batches());

			if (exactBudgetExceeded) {
				System.out.printf(
						"  %6d tx -> FFD: %4d batches in %8s | BFD: %4d batches in %8s | BFD+LS: %4d batches in %8s | exact skipped beyond %d ms budget%n",
						size, ffdResult.batches().size(), ffdResult.elapsedDisplay(), bfdResult.batches().size(),
						bfdResult.elapsedDisplay(), bfdLocalSearchResult.batches().size(),
						bfdLocalSearchResult.elapsedDisplay(), EXACT_TIME_BUDGET.toMillis());
				continue;
			}

			BenchmarkResult exactResult = benchmark(exactPlanner, transactions);
			assertBatchesRespectLimit(exactResult.batches());
			assertTrue(exactResult.batches().size() <= ffdResult.batches().size());
			assertTrue(exactResult.batches().size() <= bfdResult.batches().size());
			assertTrue(exactResult.batches().size() <= bfdLocalSearchResult.batches().size());

			int ffdImprovement = ffdResult.batches().size() - exactResult.batches().size();
			int bfdImprovement = bfdResult.batches().size() - exactResult.batches().size();
			int bfdLocalSearchImprovement = bfdLocalSearchResult.batches().size() - exactResult.batches().size();
			double exactToFfdRuntimeRatio = exactResult.elapsedNanos() / (double) Math.max(1L, ffdResult.elapsedNanos());
			double exactToBfdRuntimeRatio = exactResult.elapsedNanos() / (double) Math.max(1L, bfdResult.elapsedNanos());
			double exactToBfdLocalSearchRuntimeRatio = exactResult.elapsedNanos()
					/ (double) Math.max(1L, bfdLocalSearchResult.elapsedNanos());
			System.out.printf(
					"  %6d tx -> FFD: %4d batches in %8s | BFD: %4d batches in %8s | BFD+LS: %4d batches in %8s | exact: %4d batches in %8s | delta vs FFD: %+d | delta vs BFD: %+d | delta vs BFD+LS: %+d | exact %.1fx slower than FFD | exact %.1fx slower than BFD | exact %.1fx slower than BFD+LS%n",
					size, ffdResult.batches().size(), ffdResult.elapsedDisplay(), bfdResult.batches().size(),
					bfdResult.elapsedDisplay(), bfdLocalSearchResult.batches().size(),
					bfdLocalSearchResult.elapsedDisplay(), exactResult.batches().size(), exactResult.elapsedDisplay(),
					ffdImprovement, bfdImprovement, bfdLocalSearchImprovement, exactToFfdRuntimeRatio,
					exactToBfdRuntimeRatio, exactToBfdLocalSearchRuntimeRatio);

			if (exactResult.elapsed().compareTo(EXACT_TIME_BUDGET) > 0) {
				exactBudgetExceeded = true;
				System.out.printf("      exact planner budget exceeded at %d transactions in scenario %s%n", size,
						scenarioName);
			}
		}
	}

	private BenchmarkResult benchmark(SubmissionBatchPlanner planner, List<Transaction> transactions) {
		long startedAt = System.nanoTime();
		List<AuditBatch> batches = planner.plan(transactions, MAX_BATCH_TOTAL);
		Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
		return new BenchmarkResult(batches, elapsed);
	}

	private void assertBatchesRespectLimit(List<AuditBatch> batches) {
		assertTrue(batches.stream().allMatch(batch -> batch.totalValueOfAllTransactions().compareTo(MAX_BATCH_TOTAL) <= 0));
	}

	private List<Transaction> buildTransactions(List<String> repeatingPattern, int size) {
		List<Transaction> transactions = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			transactions.add(new Transaction(UUID.randomUUID(),
					new BigDecimal(repeatingPattern.get(index % repeatingPattern.size()))));
		}
		return List.copyOf(transactions);
	}

	private record BenchmarkResult(List<AuditBatch> batches, Duration elapsed) {
		private long elapsedNanos() {
			return elapsed.toNanos();
		}

		private String elapsedDisplay() {
			long nanos = elapsed.toNanos();
			double micros = nanos / 1_000.0;
			if (micros < 1_000.0) {
				return String.format("%.0f us", micros);
			}

			double millis = nanos / 1_000_000.0;
			if (millis < 1_000.0) {
				return String.format("%.3f ms", millis);
			}

			double seconds = nanos / 1_000_000_000.0;
			if (seconds < 60.0) {
				return String.format("%.3f s", seconds);
			}

			double minutes = seconds / 60.0;
			return String.format("%.3f m", minutes);
		}
	}
}
