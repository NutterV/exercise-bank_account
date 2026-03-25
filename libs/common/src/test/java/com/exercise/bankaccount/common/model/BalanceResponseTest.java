package com.exercise.bankaccount.common.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BalanceResponseTest {
	@Test
	void shouldExposeBalanceValue() {
		BalanceResponse response = new BalanceResponse(123.45);

		assertThat(response.balance()).isEqualTo(123.45);
	}
}
