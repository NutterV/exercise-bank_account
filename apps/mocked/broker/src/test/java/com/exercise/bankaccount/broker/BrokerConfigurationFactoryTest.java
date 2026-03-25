package com.exercise.bankaccount.broker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.activemq.artemis.core.config.Configuration;
import org.junit.jupiter.api.Test;

class BrokerConfigurationFactoryTest {
	@Test
	void shouldCreateNonPersistentBrokerConfiguration() throws Exception {
		BrokerRuntimeProperties properties = new BrokerRuntimeProperties("localhost", 61616, 7);

		Configuration configuration = new BrokerConfigurationFactory().create(properties);

		assertFalse(configuration.isPersistenceEnabled());
		assertFalse(configuration.isSecurityEnabled());
		assertEquals(2, configuration.getAcceptorConfigurations().size());
	}
}
