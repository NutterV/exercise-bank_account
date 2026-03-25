package com.exercise.bankaccount.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditRuntimePropertiesTest {

	@Test
	void shouldDefaultBrokerUrlAndQueue() {
		AuditRuntimeProperties properties = new AuditRuntimeProperties(null, "");

		assertEquals("tcp://127.0.0.1:61616", properties.brokerUrl());
		assertEquals("bank-account.audit", properties.auditQueue());
	}
}
