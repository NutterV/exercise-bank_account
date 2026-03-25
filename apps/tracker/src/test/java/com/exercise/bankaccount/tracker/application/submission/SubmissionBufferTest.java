package com.exercise.bankaccount.tracker.application.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exercise.bankaccount.common.model.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubmissionBufferTest {
	@Test
	void shouldAppendSealAndThenReportFull() {
		SubmissionBuffer buffer = new SubmissionBuffer(7, 2);
		Transaction first = transaction("100");
		Transaction second = transaction("200");
		Transaction third = transaction("300");

		assertEquals(AppendResult.APPENDED, buffer.tryAppend(first));
		assertEquals(AppendResult.SEALED, buffer.tryAppend(second));
		assertEquals(AppendResult.FULL, buffer.tryAppend(third));
		assertEquals(List.of(first, second), buffer.toList());
	}

	@Test
	void shouldResetBufferStateForReuse() {
		SubmissionBuffer buffer = new SubmissionBuffer(3, 2);
		buffer.tryAppend(transaction("100"));
		buffer.markDispatched();

		buffer.reset();

		assertTrue(buffer.markDispatched());
		assertEquals(List.of(), buffer.toList());
		assertEquals(AppendResult.APPENDED, buffer.tryAppend(transaction("50")));
	}

	private static Transaction transaction(String amount) {
		return new Transaction(UUID.randomUUID(), new BigDecimal(amount));
	}
}
