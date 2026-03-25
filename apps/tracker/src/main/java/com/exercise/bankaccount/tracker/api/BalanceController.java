package com.exercise.bankaccount.tracker.api;

import com.exercise.bankaccount.common.model.BalanceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for querying the current tracked bank-account balance.
 */
@RestController
@RequestMapping("/balances")
public class BalanceController {
	private final BankAccountService bankAccountService;

	/**
	 * @param bankAccountService
	 *            service that owns the live balance state
	 */
	public BalanceController(BankAccountService bankAccountService) {
		this.bankAccountService = bankAccountService;
	}

	/**
	 * Returns the current account balance snapshot.
	 *
	 * @return serialized balance response for the tracked account
	 */
	@GetMapping("/current")
	public BalanceResponse currentBalance() {
		return new BalanceResponse(bankAccountService.retrieveBalance());
	}
}
