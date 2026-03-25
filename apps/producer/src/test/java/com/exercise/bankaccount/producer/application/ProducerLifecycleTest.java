package com.exercise.bankaccount.producer.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.exercise.bankaccount.producer.domain.TransactionFactory;
import com.exercise.bankaccount.producer.utils.SlidingWindowRateLimiter;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

class ProducerLifecycleTest {
	@Test
	void shouldStartBothWorkersWhenApplicationRuns() throws Exception {
		RecordingWorker creditWorker = new RecordingWorker();
		RecordingWorker debitWorker = new RecordingWorker();
		ProducerLifecycle lifecycle = new ProducerLifecycle(creditWorker, debitWorker);

		lifecycle.run(new NoOpApplicationArguments());

		assertThat(creditWorker.awaitRun()).isTrue();
		assertThat(debitWorker.awaitRun()).isTrue();
		lifecycle.stop();
	}

	@Test
	void shouldStopWorkersEvenIfExecutorWasNeverStarted() {
		RecordingWorker creditWorker = new RecordingWorker();
		RecordingWorker debitWorker = new RecordingWorker();
		ProducerLifecycle lifecycle = new ProducerLifecycle(creditWorker, debitWorker);

		lifecycle.stop();

		assertThat(creditWorker.stopCalls()).isEqualTo(1);
		assertThat(debitWorker.stopCalls()).isEqualTo(1);
	}

	private static final class RecordingWorker extends TransactionProducerWorker {
		private final CountDownLatch ran = new CountDownLatch(1);
		private int stopCalls;

		private RecordingWorker() {
			super("test-worker", TransactionDirection.CREDIT, new SlidingWindowRateLimiter(1, 1_000L),
					new TransactionFactory(BigDecimal.ONE, BigDecimal.TEN), new NoOpTransactionPublisher());
		}

		@Override
		public void run() {
			ran.countDown();
		}

		@Override
		public void stop() {
			stopCalls++;
		}

		private boolean awaitRun() throws InterruptedException {
			return ran.await(1, TimeUnit.SECONDS);
		}

		private int stopCalls() {
			return stopCalls;
		}
	}

	private static final class NoOpTransactionPublisher extends TransactionPublisher {
		private NoOpTransactionPublisher() {
			super(null, null, null);
		}

		@Override
		public void publish(com.exercise.bankaccount.common.model.Transaction transaction) {
		}
	}

	private static final class NoOpApplicationArguments implements ApplicationArguments {
		@Override
		public String[] getSourceArgs() {
			return new String[0];
		}

		@Override
		public java.util.Set<String> getOptionNames() {
			return java.util.Set.of();
		}

		@Override
		public boolean containsOption(String name) {
			return false;
		}

		@Override
		public java.util.List<String> getOptionValues(String name) {
			return null;
		}

		@Override
		public java.util.List<String> getNonOptionArgs() {
			return java.util.List.of();
		}
	}
}
