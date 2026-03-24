package com.exercise.bankaccount.tracker.application;

import com.exercise.bankaccount.common.api.BankAccountService;
import com.exercise.bankaccount.common.model.Transaction;
import java.util.concurrent.atomic.DoubleAdder;
import org.springframework.stereotype.Service;

@Service
public class InMemoryBankAccountService implements BankAccountService {

    private final DoubleAdder balance = new DoubleAdder();

    @Override
    public void processTransaction(Transaction transaction) {
        balance.add(transaction.amount().doubleValue());
    }

    @Override
    public double retrieveBalance() {
        return balance.sum();
    }
}
