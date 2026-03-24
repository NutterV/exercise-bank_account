package com.exercise.bankaccount.audit;

import com.exercise.bankaccount.common.model.AuditSubmissionEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Formats audit submissions as pretty-printed JSON matching the exercise's mocked audit output.
 */
final class AuditConsoleFormatter {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT);

    String format(AuditSubmissionEnvelope envelope) throws Exception {
        return objectMapper.writeValueAsString(envelope);
    }
}
