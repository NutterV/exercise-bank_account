package com.exercise.bankaccount.producer.application;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

/**
 * Starts the dedicated producer threads required by the exercise and shuts them
 * down with the application.
 */
@Service
public class ProducerLifecycle implements ApplicationRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProducerLifecycle.class);

	private final TransactionProducerWorker creditWorker;
	private final TransactionProducerWorker debitWorker;
	private ExecutorService executorService;

	public ProducerLifecycle(@Qualifier("creditWorker") TransactionProducerWorker creditWorker,
			@Qualifier("debitWorker") TransactionProducerWorker debitWorker) {
		this.creditWorker = creditWorker;
		this.debitWorker = debitWorker;
	}

	@Override
	public void run(ApplicationArguments args) {
		executorService = Executors.newFixedThreadPool(2);
		executorService.submit(creditWorker);
		executorService.submit(debitWorker);
		LOGGER.info("Producer started dedicated credit and debit threads");
	}

	@PreDestroy
	void stop() {
		creditWorker.stop();
		debitWorker.stop();

		if (executorService == null) {
			return;
		}

		executorService.shutdownNow();
		try {
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				LOGGER.warn("Producer workers did not stop within the termination timeout");
			}
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
		}
	}
}
