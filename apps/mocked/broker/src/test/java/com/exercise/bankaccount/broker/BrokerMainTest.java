package com.exercise.bankaccount.broker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.junit.jupiter.api.Test;

class BrokerMainTest {
	@Test
	void shouldAllowPrivateConstructorForUtilityEntryPoint() throws Exception {
		Constructor<BrokerMain> constructor = BrokerMain.class.getDeclaredConstructor();
		constructor.setAccessible(true);

		assertDoesNotThrow(() -> constructor.newInstance());
	}

	@Test
	void shouldStopBrokerWithoutPropagatingExceptions() throws Exception {
		Method stopBroker = BrokerMain.class.getDeclaredMethod("stopBroker", EmbeddedActiveMQ.class);
		stopBroker.setAccessible(true);

		assertDoesNotThrow(() -> stopBroker.invoke(null, new TrackingEmbeddedActiveMq(false)));
		assertDoesNotThrow(() -> stopBroker.invoke(null, new TrackingEmbeddedActiveMq(true)));
	}

	private static final class TrackingEmbeddedActiveMq extends EmbeddedActiveMQ {
		private final boolean failOnStop;

		private TrackingEmbeddedActiveMq(boolean failOnStop) {
			this.failOnStop = failOnStop;
		}

		@Override
		public EmbeddedActiveMQ stop() throws Exception {
			if (failOnStop) {
				throw new IllegalStateException("boom");
			}
			return this;
		}
	}
}
