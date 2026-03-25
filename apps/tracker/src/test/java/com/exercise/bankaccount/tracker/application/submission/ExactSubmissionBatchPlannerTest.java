package com.exercise.bankaccount.tracker.application.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExactSubmissionBatchPlannerTest {
	private final ExactSubmissionBatchPlanner planner = new ExactSubmissionBatchPlanner();
	private final FirstFitDecreasingSubmissionBatchPlanner ffdPlanner = new FirstFitDecreasingSubmissionBatchPlanner();

	@Test
	void shouldFindFewerBatchesThanFirstFitDecreasingForKnownCounterExample() {
		List<Transaction> transactions = List.of(transaction("6"), transaction("5"), transaction("3"), transaction("2"),
				transaction("2"), transaction("2"));

		List<AuditBatch> exactBatches = planner.plan(transactions, BigDecimal.TEN);
		List<AuditBatch> ffdBatches = ffdPlanner.plan(transactions, BigDecimal.TEN);

		assertEquals(2, exactBatches.size());
		assertEquals(3, ffdBatches.size());
		assertEquals(new AuditBatch(new BigDecimal("10"), 3), exactBatches.get(0));
		assertEquals(new AuditBatch(new BigDecimal("10"), 3), exactBatches.get(1));
	}

	@Test
	void shouldMatchFirstFitDecreasingWhenThatPackingIsAlreadyOptimal() {
		List<Transaction> transactions = List.of(transaction("600000"), transaction("400000"), transaction("600000"),
				transaction("400000"));

		List<AuditBatch> exactBatches = planner.plan(transactions, BigDecimal.valueOf(1_000_000));
		List<AuditBatch> ffdBatches = ffdPlanner.plan(transactions, BigDecimal.valueOf(1_000_000));

		assertEquals(ffdBatches, exactBatches);
	}

	@Test
	void shouldRejectTransactionsThatExceedTheBatchLimit() {
		assertThrows(IllegalArgumentException.class,
				() -> planner.plan(List.of(transaction("1000000.01")), BigDecimal.valueOf(1_000_000)));
	}

	private static Transaction transaction(String amount) {
		return new Transaction(UUID.randomUUID(), new BigDecimal(amount));
	}
}
