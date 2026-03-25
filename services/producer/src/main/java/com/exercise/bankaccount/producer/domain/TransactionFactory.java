package com.exercise.bankaccount.producer.domain;

import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.producer.application.TransactionDirection;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates exercise-compliant transactions.
 */
public class TransactionFactory {

	private final long minimumAmount;
	private final long maximumAmount;

	public TransactionFactory(BigDecimal minimumAmount, BigDecimal maximumAmount) {
		this.minimumAmount = minimumAmount.longValueExact();
		this.maximumAmount = maximumAmount.longValueExact();
	}

	public Transaction create(TransactionDirection direction) {
		long amount = ThreadLocalRandom.current().nextLong(minimumAmount, maximumAmount + 1);
		BigDecimal signedAmount = BigDecimal.valueOf(direction == TransactionDirection.DEBIT ? -amount : amount);
		return new Transaction(UUID.randomUUID(), signedAmount);
	}
}
