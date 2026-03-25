package com.exercise.bankaccount.common.model;

import java.util.List;
import java.util.Objects;

/**
 * Audit submission payload containing the optimized batches derived from
 * exactly one submission window.
 *
 * @param batches
 *            ordered batches sent to the mocked audit system
 */
public record AuditSubmission(List<AuditBatch> batches) {
	public AuditSubmission {
		batches = List.copyOf(Objects.requireNonNull(batches, "batches must not be null"));
	}
}
