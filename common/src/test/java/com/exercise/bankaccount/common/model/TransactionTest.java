package com.exercise.bankaccount.common.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionTest {

    @Test
    void shouldExposeAbsoluteAmount() {
        Transaction transaction = new Transaction(UUID.randomUUID(), BigDecimal.valueOf(-250.75));

        assertThat(transaction.absoluteAmount()).isEqualByComparingTo("250.75");
    }
}
