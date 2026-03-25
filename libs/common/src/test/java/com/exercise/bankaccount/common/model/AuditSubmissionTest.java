package com.exercise.bankaccount.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditSubmissionTest {
	@Test
	void shouldRejectNullBatchList() {
		assertThatThrownBy(() -> new AuditSubmission(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("batches must not be null");
	}

	@Test
	void shouldDefensivelyCopyBatches() {
		List<AuditBatch> source = new ArrayList<>();
		source.add(new AuditBatch(BigDecimal.ONE, 1));

		AuditSubmission submission = new AuditSubmission(source);
		source.add(new AuditBatch(BigDecimal.TEN, 2));

		assertThat(submission.batches()).hasSize(1);
		assertThatThrownBy(() -> submission.batches().add(new AuditBatch(BigDecimal.ZERO, 0)))
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
