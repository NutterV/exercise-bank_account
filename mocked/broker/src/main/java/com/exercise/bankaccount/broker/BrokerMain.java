package com.exercise.bankaccount.broker;

import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Starts and keeps alive the mocked embedded Artemis broker process used for local integration.
 */
public final class BrokerMain {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrokerMain.class);
	private static final CountDownLatch SHUTDOWN_LATCH = new CountDownLatch(1);

	private BrokerMain() {
	}

	/**
	 * Boots the embedded broker using runtime properties resolved from system properties or environment variables.
	 *
	 * @param args unused command-line arguments
	 * @throws Exception if broker configuration or startup fails
	 */
	public static void main(String[] args) throws Exception {
		BrokerRuntimeProperties properties = BrokerRuntimeProperties.fromSystem();
		EmbeddedActiveMQ broker = new EmbeddedActiveMQ();
		broker.setConfiguration(new BrokerConfigurationFactory().create(properties));
		broker.start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> stopBroker(broker), "broker-shutdown"));
		LOGGER.info("Embedded broker started on {}", properties.tcpAcceptorUri());
		LOGGER.info("Embedded broker also exposes {}", properties.vmAcceptorUri());

		SHUTDOWN_LATCH.await();
	}

	/**
	 * Stops the embedded broker during JVM shutdown.
	 *
	 * @param broker running broker instance
	 */
	private static void stopBroker(EmbeddedActiveMQ broker) {
		try {
			LOGGER.info("Stopping embedded broker");
			broker.stop();
		} catch (Exception exception) {
			LOGGER.error("Failed to stop embedded broker cleanly", exception);
		}
	}
}
