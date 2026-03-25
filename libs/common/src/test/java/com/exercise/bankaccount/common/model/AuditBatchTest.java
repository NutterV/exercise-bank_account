package com.exercise.bankaccount.common.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AuditBatchTest {
	@Test
	void shouldRejectNullTotalValue() {
		assertThatThrownBy(() -> new AuditBatch(null, 1))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("totalValueOfAllTransactions must not be null");
	}

	@Test
	void shouldKeepProvidedValues() {
		AuditBatch batch = new AuditBatch(BigDecimal.TEN, 3);

		org.assertj.core.api.Assertions.assertThat(batch.totalValueOfAllTransactions()).isEqualByComparingTo("10");
		org.assertj.core.api.Assertions.assertThat(batch.countOfTransactions()).isEqualTo(3);
	}
}
