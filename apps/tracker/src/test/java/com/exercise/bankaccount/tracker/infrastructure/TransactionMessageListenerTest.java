package com.exercise.bankaccount.tracker.infrastructure;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.tracker.api.BankAccountService;
import com.exercise.bankaccount.tracker.application.performance.TrackerPerformanceCaptureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TransactionMessageListenerTest {
	@Test
	void shouldDeserializeTransactionAndForwardToService() throws Exception {
		BankAccountService bankAccountService = Mockito.mock(BankAccountService.class);
		TrackerPerformanceCaptureService trackerPerformanceCaptureService = Mockito
				.mock(TrackerPerformanceCaptureService.class);
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		TransactionMessageListener listener = new TransactionMessageListener(bankAccountService, objectMapper,
				trackerPerformanceCaptureService);
		Transaction transaction = new Transaction(UUID.randomUUID(), BigDecimal.valueOf(250));
		TextMessage message = Mockito.mock(TextMessage.class);
		when(message.getText()).thenReturn(objectMapper.writeValueAsString(transaction));

		listener.onMessage(message);

		verify(trackerPerformanceCaptureService).recordTransactionConsumed();
		verify(bankAccountService).processTransaction(transaction);
	}

	@Test
	void shouldRejectUnsupportedMessageTypesWithoutCallingService() {
		BankAccountService bankAccountService = Mockito.mock(BankAccountService.class);
		TrackerPerformanceCaptureService trackerPerformanceCaptureService = Mockito
				.mock(TrackerPerformanceCaptureService.class);
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		TransactionMessageListener listener = new TransactionMessageListener(bankAccountService, objectMapper,
				trackerPerformanceCaptureService);
		Message message = Mockito.mock(Message.class);

		org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> listener.onMessage(message));
		verifyNoInteractions(bankAccountService, trackerPerformanceCaptureService);
	}
}
