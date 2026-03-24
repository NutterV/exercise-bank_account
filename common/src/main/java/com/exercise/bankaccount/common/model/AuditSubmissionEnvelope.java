package com.exercise.bankaccount.common.model;

import java.util.Objects;

/**
 * Top-level payload used when printing or transporting a mocked audit submission.
 *
 * @param submission audit submission to expose to the downstream mocked audit system
 */
public record AuditSubmissionEnvelope(AuditSubmission submission) {

    public AuditSubmissionEnvelope {
        Objects.requireNonNull(submission, "submission must not be null");
    }
}
