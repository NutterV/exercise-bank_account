package com.exercise.bankaccount.common.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record Transaction(UUID id, BigDecimal amount) {

	public Transaction {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
	}

	public BigDecimal absoluteAmount() {
		return amount.abs();
	}
}
