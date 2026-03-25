package com.exercise.bankaccount.tracker.application;

import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.tracker.api.BankAccountService;
import com.exercise.bankaccount.tracker.application.submission.SubmissionBufferCoordinator;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

/**
 * Tracks the live balance immediately while delegating submission-buffer
 * coordination to a dedicated component.
 */
@Service
public class InMemoryBankAccountService implements BankAccountService {
	private final AtomicReference<BigDecimal> balance = new AtomicReference<>(BigDecimal.ZERO);
	private final SubmissionBufferCoordinator submissionBufferCoordinator;

	/**
	 * Creates the tracker service that updates the live balance and delegates
	 * submission-window handling.
	 *
	 * @param submissionBufferCoordinator
	 *            coordinator that owns submission-buffer rotation and dispatch
	 */
	public InMemoryBankAccountService(SubmissionBufferCoordinator submissionBufferCoordinator) {
		this.submissionBufferCoordinator = submissionBufferCoordinator;
	}

	/**
	 * Updates the live balance immediately and records the transaction into the
	 * current submission window.
	 *
	 * @param transaction
	 *            transaction to ingest
	 */
	@Override
	public void processTransaction(Transaction transaction) {
		balance.updateAndGet(currentBalance -> currentBalance.add(transaction.amount()));
		submissionBufferCoordinator.record(transaction);
	}

	/**
	 * Returns the current balance accumulated from all ingested transactions.
	 *
	 * @return current live balance
	 */
	@Override
	public double retrieveBalance() {
		return balance.get().doubleValue();
	}
}
