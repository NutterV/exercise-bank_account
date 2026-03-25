package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.Transaction;
import java.math.BigDecimal;
import java.util.List;

/**
 * Plans audit batches for one sealed submission window.
 */
interface SubmissionBatchPlanner {
	/**
	 * Builds the audit batches for one completed submission window.
	 *
	 * @param transactions
	 *            sealed submission transactions
	 * @param maxBatchTotal
	 *            configured maximum absolute total per audit batch
	 * @return audit batches for the submission window
	 */
	List<AuditBatch> plan(List<Transaction> transactions, BigDecimal maxBatchTotal);
}
