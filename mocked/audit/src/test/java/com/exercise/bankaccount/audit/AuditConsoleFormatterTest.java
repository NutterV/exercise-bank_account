package com.exercise.bankaccount.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.AuditSubmission;
import com.exercise.bankaccount.common.model.AuditSubmissionEnvelope;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditConsoleFormatterTest {

    @Test
    void shouldFormatSubmissionUsingExerciseShape() throws Exception {
        AuditSubmissionEnvelope envelope = new AuditSubmissionEnvelope(
                new AuditSubmission(List.of(
                        new AuditBatch(BigDecimal.valueOf(75000), 18),
                        new AuditBatch(BigDecimal.valueOf(98000), 12)
                ))
        );

        String formatted = new AuditConsoleFormatter().format(envelope);

        assertTrue(formatted.contains("\"submission\""));
        assertTrue(formatted.contains("\"batches\""));
        assertTrue(formatted.contains("\"totalValueOfAllTransactions\""));
        assertTrue(formatted.contains("\"countOfTransactions\""));
    }
}
