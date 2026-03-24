package com.exercise.bankaccount.tracker.api;

import com.exercise.bankaccount.common.api.BankAccountService;
import com.exercise.bankaccount.common.model.BalanceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/balances")
public class BalanceController {

    private final BankAccountService bankAccountService;

    public BalanceController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping("/current")
    public BalanceResponse currentBalance() {
        return new BalanceResponse(bankAccountService.retrieveBalance());
    }
}
