package com.exercise.bankaccount.common.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditSubmissionEnvelopeTest {

    @Test
    void shouldDefensivelyCopyAuditBatches() {
        AuditBatch batch = new AuditBatch(BigDecimal.valueOf(75000), 18);
        AuditSubmission submission = new AuditSubmission(List.of(batch));
        AuditSubmissionEnvelope envelope = new AuditSubmissionEnvelope(submission);

        assertEquals(1, envelope.submission().batches().size());
        assertEquals(BigDecimal.valueOf(75000), envelope.submission().batches().getFirst().totalValueOfAllTransactions());
    }
}
