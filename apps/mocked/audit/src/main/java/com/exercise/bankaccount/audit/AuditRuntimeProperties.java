package com.exercise.bankaccount.audit;

/**
 * Runtime settings for the mocked audit application.
 *
 * @param brokerUrl
 *            Artemis broker TCP endpoint
 * @param auditQueue
 *            queue from which audit submissions are consumed
 */
record AuditRuntimeProperties(String brokerUrl, String auditQueue) {
	private static final String BROKER_URL_PROPERTY = "bank.account.audit.broker-url";
	private static final String AUDIT_QUEUE_PROPERTY = "bank.account.audit.queue";

	private static final String BROKER_URL_ENV = "BANK_ACCOUNT_AUDIT_BROKER_URL";
	private static final String AUDIT_QUEUE_ENV = "BANK_ACCOUNT_AUDIT_QUEUE";

	AuditRuntimeProperties {
		brokerUrl = brokerUrl == null || brokerUrl.isBlank() ? "tcp://127.0.0.1:61616" : brokerUrl;
		auditQueue = auditQueue == null || auditQueue.isBlank() ? "bank-account.audit" : auditQueue;
	}

	static AuditRuntimeProperties fromSystem() {
		return new AuditRuntimeProperties(readString(BROKER_URL_PROPERTY, BROKER_URL_ENV),
				readString(AUDIT_QUEUE_PROPERTY, AUDIT_QUEUE_ENV));
	}

	private static String readString(String propertyName, String environmentName) {
		String propertyValue = System.getProperty(propertyName);
		if (propertyValue != null && !propertyValue.isBlank()) {
			return propertyValue;
		}

		String environmentValue = System.getenv(environmentName);
		return environmentValue == null || environmentValue.isBlank() ? null : environmentValue;
	}
}
