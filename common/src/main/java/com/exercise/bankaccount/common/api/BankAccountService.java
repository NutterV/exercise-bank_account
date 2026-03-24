package com.exercise.bankaccount.common.api;

import com.exercise.bankaccount.common.model.Transaction;

/**
 * Service to aggregate transactions tracking the overall balance for an account.
 */
public interface BankAccountService {

    /**
     * Process a given transaction.
     *
     * @param transaction transaction to process
     */
    void processTransaction(Transaction transaction);

    /**
     * Retrieve the balance in the account.
     *
     * @return current account balance
     */
    double retrieveBalance();
}
