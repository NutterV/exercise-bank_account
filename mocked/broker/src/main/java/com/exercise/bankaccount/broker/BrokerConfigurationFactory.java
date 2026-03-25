package com.exercise.bankaccount.broker;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;

/**
 * Creates the Artemis server configuration for the mocked embedded broker.
 */
final class BrokerConfigurationFactory {

	/**
	 * Builds a non-persistent, unsecured broker configuration with both in-vm and TCP acceptors.
	 *
	 * @param properties runtime properties that define broker endpoints
	 * @return configured Artemis server configuration
	 * @throws Exception if Artemis rejects the supplied acceptor configuration
	 */
	Configuration create(BrokerRuntimeProperties properties) throws Exception {
		Configuration configuration = new ConfigurationImpl()
			                              .setPersistenceEnabled(false)
			                              .setSecurityEnabled(false);

		configuration.addAcceptorConfiguration("in-vm", properties.vmAcceptorUri());
		configuration.addAcceptorConfiguration("tcp", properties.tcpAcceptorUri());

		return configuration;
	}
}
