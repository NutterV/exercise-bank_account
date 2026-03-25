package com.exercise.bankaccount.tracker.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BalanceController.class)
class BalanceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BankAccountService bankAccountService;

	@Test
	void shouldExposeCurrentBalance() throws Exception {
		given(bankAccountService.retrieveBalance()).willReturn(125.5d);

		mockMvc.perform(get("/balances/current"))
		       .andExpect(status().isOk())
		       .andExpect(content().json("""
		                                 {"balance":125.5}
		                                 """));
	}
}
