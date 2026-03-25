package com.exercise.bankaccount.audit;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.AuditSubmission;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditConsoleFormatterTest {

	@Test
	void shouldFormatSubmissionUsingExerciseShape() throws Exception {
		AuditSubmission submission = new AuditSubmission(List.of(
			new AuditBatch(BigDecimal.valueOf(75000), 18),
			new AuditBatch(BigDecimal.valueOf(98000), 12)
		));

		String formatted = AuditConsoleFormatter.format(submission);

		assertTrue(formatted.contains("\"submission\""));
		assertTrue(formatted.contains("\"batches\""));
		assertTrue(formatted.contains("\"totalValueOfAllTransactions\""));
		assertTrue(formatted.contains("\"countOfTransactions\""));
	}
}
