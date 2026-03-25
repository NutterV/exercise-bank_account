package com.exercise.bankaccount.producer.application;

import com.exercise.bankaccount.producer.domain.TransactionFactory;
import com.exercise.bankaccount.producer.utils.SlidingWindowRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the dedicated credit and debit producer workers.
 */
@Configuration
public class TransactionWorkerConfiguration {
	@Bean
	TransactionFactory transactionFactory(ProducerProperties producerProperties) {
		return new TransactionFactory(producerProperties.minimumAmount(), producerProperties.maximumAmount());
	}

	@Bean
	TransactionProducerWorker creditWorker(ProducerProperties producerProperties, TransactionFactory transactionFactory,
			TransactionPublisher transactionPublisher) {
		return new TransactionProducerWorker("producer-credit-thread", TransactionDirection.CREDIT,
				new SlidingWindowRateLimiter(producerProperties.creditsPerSecond(), 1_000L), transactionFactory,
				transactionPublisher);
	}

	@Bean
	TransactionProducerWorker debitWorker(ProducerProperties producerProperties, TransactionFactory transactionFactory,
			TransactionPublisher transactionPublisher) {
		return new TransactionProducerWorker("producer-debit-thread", TransactionDirection.DEBIT,
				new SlidingWindowRateLimiter(producerProperties.debitsPerSecond(), 1_000L), transactionFactory,
				transactionPublisher);
	}
}
