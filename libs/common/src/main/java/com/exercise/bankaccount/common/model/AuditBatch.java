package com.exercise.bankaccount.common.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Summary of one batch inside an audit submission.
 *
 * @param totalValueOfAllTransactions absolute total value across all transactions in the batch
 * @param countOfTransactions         number of transactions contained in the batch
 */
public record AuditBatch(BigDecimal totalValueOfAllTransactions, int countOfTransactions) {

	public AuditBatch {
		Objects.requireNonNull(totalValueOfAllTransactions, "totalValueOfAllTransactions must not be null");
	}
}
