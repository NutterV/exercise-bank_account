package com.exercise.bankaccount.commonservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bank-account.messaging")
public record MessagingProperties(String transactionQueue, String auditQueue) {

	public MessagingProperties {
		transactionQueue = transactionQueue == null || transactionQueue.isBlank()
		                   ? "bank-account.transactions"
		                   : transactionQueue;
		auditQueue = auditQueue == null || auditQueue.isBlank()
		             ? "bank-account.audit"
		             : auditQueue;
	}
}
