package com.exercise.bankaccount.commonservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MessagingPropertiesTest {
	@Test
	void shouldProvideDefaultQueueNames() {
		MessagingProperties properties = new MessagingProperties(null);

		assertThat(properties.queueName(QueueType.TRANSACTION)).isEqualTo("bank-account.transactions");
		assertThat(properties.queueName(QueueType.AUDIT)).isEqualTo("bank-account.audit");
	}

	@Test
	void shouldLookupConfiguredQueueByType() {
		MessagingProperties properties = new MessagingProperties(
				Map.of(QueueType.TRANSACTION, "transactions.queue", QueueType.AUDIT, "audit.queue"));

		assertThat(properties.queueName(QueueType.TRANSACTION)).isEqualTo("transactions.queue");
		assertThat(properties.queueName(QueueType.AUDIT)).isEqualTo("audit.queue");
	}
}
