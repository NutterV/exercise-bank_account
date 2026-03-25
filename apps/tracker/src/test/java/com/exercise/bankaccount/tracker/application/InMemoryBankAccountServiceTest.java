package com.exercise.bankaccount.tracker.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.tracker.application.config.TrackerSubmissionProperties;
import com.exercise.bankaccount.tracker.application.submission.SubmissionBufferCoordinator;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryBankAccountServiceTest {
	@Test
	void shouldKeepRunningBalanceOnAtomicAccumulatorWhileDelegatingSubmissionHandling() {
		RecordingSubmissionBufferCoordinator coordinator = new RecordingSubmissionBufferCoordinator();
		InMemoryBankAccountService service = new InMemoryBankAccountService(coordinator);

		Transaction credit = transaction(BigDecimal.valueOf(125.75));
		Transaction debit = transaction(BigDecimal.valueOf(-25.25));

		service.processTransaction(credit);
		service.processTransaction(debit);

		assertEquals(100.5d, service.retrieveBalance());
		assertEquals(2, coordinator.recordedTransactions().size());
		assertEquals(credit, coordinator.recordedTransactions().get(0));
		assertEquals(debit, coordinator.recordedTransactions().get(1));
	}

	@Test
	void shouldAccumulateDecimalAmountsWithoutFloatingPointDrift() {
		RecordingSubmissionBufferCoordinator coordinator = new RecordingSubmissionBufferCoordinator();
		InMemoryBankAccountService service = new InMemoryBankAccountService(coordinator);

		for (int index = 0; index < 10_000; index++) {
			service.processTransaction(transaction(new BigDecimal("0.01")));
		}

		assertEquals(100.0d, service.retrieveBalance());
		assertEquals(10_000, coordinator.recordedTransactions().size());
	}

	private static Transaction transaction(BigDecimal amount) {
		return new Transaction(UUID.randomUUID(), amount);
	}

	@Test
	void shouldApplyDefaultSubmissionSettingsWhenConfigurationIsMissing() {
		TrackerSubmissionProperties properties = new TrackerSubmissionProperties(0, 0, 0, null);

		assertEquals(1_000, properties.submissionSize());
		assertEquals(2, properties.initialBufferCount());
		assertEquals(10, properties.maxBufferCount());
		assertEquals(new java.math.BigDecimal("1000000"), properties.maxBatchTotal());
	}

	private static final class RecordingSubmissionBufferCoordinator extends SubmissionBufferCoordinator {
		private final java.util.List<Transaction> recordedTransactions = new java.util.ArrayList<>();

		private RecordingSubmissionBufferCoordinator() {
			super(transactions -> {
			}, 1, 1, 1);
		}

		@Override
		public void record(Transaction transaction) {
			recordedTransactions.add(transaction);
		}

		private java.util.List<Transaction> recordedTransactions() {
			return recordedTransactions;
		}
	}
}
