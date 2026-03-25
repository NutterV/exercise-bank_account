package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.Transaction;
import java.util.List;

/**
 * Handles a completed 1000-transaction submission window once it has been
 * sealed off the hot path. Implementations can batch, publish, or otherwise
 * transform the completed window without blocking ingestion.
 */
public interface SubmissionProcessor {
	/**
	 * Process a completed submission window asynchronously from the
	 * balance-tracking hot path.
	 *
	 * @param transactions
	 *            exactly one sealed submission window of transactions
	 */
	void processSubmission(List<Transaction> transactions);
}
