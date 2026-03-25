package com.exercise.bankaccount.producer.application;

import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.producer.domain.TransactionFactory;
import com.exercise.bankaccount.producer.utils.SlidingWindowRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * Long-running worker that generates one transaction type and publishes it at a fixed rate.
 */
public class TransactionProducerWorker implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionProducerWorker.class);
	private static final long RATE_LIMIT_BACKOFF_NANOS = 10_000_000L;

	private final String workerName;
	private final TransactionDirection direction;
	private final SlidingWindowRateLimiter rateLimiter;
	private final TransactionFactory transactionFactory;
	private final TransactionPublisher transactionPublisher;
	private final AtomicBoolean running = new AtomicBoolean(true);

	public TransactionProducerWorker(
		String workerName,
		TransactionDirection direction,
		SlidingWindowRateLimiter rateLimiter,
		TransactionFactory transactionFactory,
		TransactionPublisher transactionPublisher
	) {
		this.workerName = workerName;
		this.direction = direction;
		this.rateLimiter = rateLimiter;
		this.transactionFactory = transactionFactory;
		this.transactionPublisher = transactionPublisher;
	}

	@Override
	public void run() {
		Thread.currentThread().setName(workerName);

		while (running.get() && !Thread.currentThread().isInterrupted()) {
			try {
				if (!rateLimiter.allow(workerName, System.currentTimeMillis())) {
					LockSupport.parkNanos(RATE_LIMIT_BACKOFF_NANOS);
					continue;
				}

				Transaction transaction = transactionFactory.create(direction);
				transactionPublisher.publish(transaction);
			} catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
			} catch (Exception exception) {
				LOGGER.error("Failed to generate or publish {} transaction", direction, exception);
			}
		}
	}

	public void stop() {
		running.set(false);
	}
}
