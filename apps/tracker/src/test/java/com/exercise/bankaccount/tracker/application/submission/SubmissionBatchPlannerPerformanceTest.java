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
	private final SubmissionBatchPlanner exactPlanner = new ExactSubmissionBatchPlanner();

	@Test
	void shouldBenchmarkPlannerTradeOffsAcrossIncreasingSubmissionSizes() {
		benchmarkScenario("production-like", List.of("500000", "-200000", "300000", "-100000"),
				List.of(4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48));
		benchmarkScenario("counterexample-rich", List.of("600000", "500000", "300000", "200000", "200000", "200000"),
				List.of(6, 12, 18, 24, 30, 36, 42, 48, 54, 60, 66, 72));
	}

	private void benchmarkScenario(String scenarioName, List<String> repeatingPattern, List<Integer> sizes) {
		System.out.printf("Planner benchmark scenario: %s%n", scenarioName);
		boolean exactBudgetExceeded = false;

		for (int size : sizes) {
			List<Transaction> transactions = buildTransactions(repeatingPattern, size);

			BenchmarkResult ffdResult = benchmark(firstFitDecreasingPlanner, transactions);
			assertBatchesRespectLimit(ffdResult.batches());

			if (exactBudgetExceeded) {
				System.out.printf("  %3d tx -> FFD: %3d batches in %4d us | exact skipped beyond %d ms budget%n", size,
						ffdResult.batches().size(), ffdResult.elapsedMicros(), EXACT_TIME_BUDGET.toMillis());
				continue;
			}

			BenchmarkResult exactResult = benchmark(exactPlanner, transactions);
			assertBatchesRespectLimit(exactResult.batches());
			assertTrue(exactResult.batches().size() <= ffdResult.batches().size());

			int improvement = ffdResult.batches().size() - exactResult.batches().size();
			System.out.printf("  %3d tx -> FFD: %3d batches in %4d us | exact: %3d batches in %4d us | delta: %+d%n",
					size, ffdResult.batches().size(), ffdResult.elapsedMicros(), exactResult.batches().size(),
					exactResult.elapsedMicros(), improvement);

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
		private long elapsedMicros() {
			return elapsed.toNanos() / 1_000;
		}
	}
}
