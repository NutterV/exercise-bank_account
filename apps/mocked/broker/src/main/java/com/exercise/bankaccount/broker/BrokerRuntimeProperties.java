package com.exercise.bankaccount.broker;

/**
 * Runtime settings for the mocked broker resolved from system properties or
 * environment variables.
 *
 * @param host
 *            TCP host exposed by the broker
 * @param port
 *            TCP port exposed by the broker
 * @param vmId
 *            in-vm broker identifier for same-JVM clients
 */
record BrokerRuntimeProperties(String host, int port, int vmId) {
	private static final String HOST_PROPERTY = "bank.account.broker.host";
	private static final String PORT_PROPERTY = "bank.account.broker.port";
	private static final String VM_ID_PROPERTY = "bank.account.broker.vm-id";

	private static final String HOST_ENV = "BANK_ACCOUNT_BROKER_HOST";
	private static final String PORT_ENV = "BANK_ACCOUNT_BROKER_PORT";
	private static final String VM_ID_ENV = "BANK_ACCOUNT_BROKER_VM_ID";

	BrokerRuntimeProperties {
		host = host == null || host.isBlank() ? "127.0.0.1" : host;
		port = port <= 0 ? 61616 : port;
		vmId = Math.max(vmId, 0);
	}

	/**
	 * Resolves broker runtime settings from JVM system properties first, then
	 * environment variables.
	 *
	 * @return normalized broker runtime properties
	 */
	static BrokerRuntimeProperties fromSystem() {
		return new BrokerRuntimeProperties(readString(HOST_PROPERTY, HOST_ENV), readInt(PORT_PROPERTY, PORT_ENV, 61616),
				readInt(VM_ID_PROPERTY, VM_ID_ENV, 0));
	}

	private static String readString(String propertyName, String environmentName) {
		String propertyValue = System.getProperty(propertyName);
		if (propertyValue != null && !propertyValue.isBlank()) {
			return propertyValue;
		}

		String environmentValue = System.getenv(environmentName);
		return environmentValue == null || environmentValue.isBlank() ? null : environmentValue;
	}

	private static int readInt(String propertyName, String environmentName, int defaultValue) {
		String rawValue = readString(propertyName, environmentName);
		if (rawValue == null) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(rawValue);
		} catch (NumberFormatException ignored) {
			return defaultValue;
		}
	}

	/**
	 * @return TCP acceptor URI for remote producer and tracker connections
	 */
	String tcpAcceptorUri() {
		return "tcp://" + host + ":" + port;
	}

	/**
	 * @return in-vm acceptor URI for same-process broker access
	 */
	String vmAcceptorUri() {
		return "vm://" + vmId;
	}
}
