package com.exercise.bankaccount.commonservice.config;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bank-account.messaging")
public record MessagingProperties(Map<QueueType, String> queues) {
	private static final Map<QueueType, String> DEFAULT_QUEUES = Map.of(QueueType.TRANSACTION,
			"bank-account.transactions", QueueType.AUDIT, "bank-account.audit");

	public MessagingProperties {
		EnumMap<QueueType, String> normalizedQueues = new EnumMap<>(QueueType.class);
		normalizedQueues.putAll(DEFAULT_QUEUES);
		if (queues != null) {
			queues.forEach((queueType, queueName) -> {
				if (queueType != null && queueName != null && !queueName.isBlank()) {
					normalizedQueues.put(queueType, queueName);
				}
			});
		}
		queues = Map.copyOf(normalizedQueues);
	}

	/**
	 * Returns the configured queue name for the given queue role.
	 *
	 * @param queueType
	 *            queue role to resolve
	 * @return configured destination name
	 */
	public String queueName(QueueType queueType) {
		return queues.get(queueType);
	}
}
