package com.exercise.bankaccount.broker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BrokerRuntimePropertiesTest {
	@Test
	void shouldBuildDefaultUris() {
		BrokerRuntimeProperties properties = new BrokerRuntimeProperties(null, 0, -1);

		assertEquals("tcp://127.0.0.1:61616", properties.tcpAcceptorUri());
		assertEquals("vm://0", properties.vmAcceptorUri());
	}
}
