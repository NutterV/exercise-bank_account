package com.exercise.bankaccount.commonservice.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingPropertiesTest {

	@Test
	void shouldProvideDefaultQueueNames() {
		MessagingProperties properties = new MessagingProperties(null, "");

		assertThat(properties.transactionQueue()).isEqualTo("bank-account.transactions");
		assertThat(properties.auditQueue()).isEqualTo("bank-account.audit");
	}
}
