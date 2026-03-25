package com.exercise.bankaccount.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuditRuntimePropertiesTest {
	@Test
	void shouldDefaultBrokerUrlAndQueue() {
		AuditRuntimeProperties properties = new AuditRuntimeProperties(null, "");

		assertEquals("tcp://127.0.0.1:61616", properties.brokerUrl());
		assertEquals("bank-account.audit", properties.auditQueue());
	}
}
