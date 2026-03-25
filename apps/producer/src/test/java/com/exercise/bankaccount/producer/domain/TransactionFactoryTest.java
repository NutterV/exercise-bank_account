package com.exercise.bankaccount.producer.domain;

import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.producer.application.TransactionDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionFactoryTest {

	private final TransactionFactory transactionFactory = new TransactionFactory(
		BigDecimal.valueOf(200),
		BigDecimal.valueOf(500000)
	);

	@Test
	void shouldCreateCreditTransactionsInConfiguredRange() {
		Transaction transaction = transactionFactory.create(TransactionDirection.CREDIT);

		assertTrue(transaction.amount().signum() > 0);
		assertTrue(transaction.amount().compareTo(BigDecimal.valueOf(200)) >= 0);
		assertTrue(transaction.amount().compareTo(BigDecimal.valueOf(500000)) <= 0);
	}

	@Test
	void shouldCreateDebitTransactionsInConfiguredRange() {
		Transaction transaction = transactionFactory.create(TransactionDirection.DEBIT);

		assertTrue(transaction.amount().signum() < 0);
		assertTrue(transaction.absoluteAmount().compareTo(BigDecimal.valueOf(200)) >= 0);
		assertTrue(transaction.absoluteAmount().compareTo(BigDecimal.valueOf(500000)) <= 0);
	}
}
