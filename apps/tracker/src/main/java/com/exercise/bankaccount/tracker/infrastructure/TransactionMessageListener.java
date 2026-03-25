package com.exercise.bankaccount.tracker.infrastructure;

import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.commonservice.config.MessagingProperties;
import com.exercise.bankaccount.tracker.api.BankAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Consumes transaction messages from the broker and hands them to the tracker service.
 */
@Component
public class TransactionMessageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMessageListener.class);

	private final BankAccountService bankAccountService;
	private final ObjectMapper objectMapper;

	public TransactionMessageListener(BankAccountService bankAccountService, ObjectMapper objectMapper) {
		this.bankAccountService = bankAccountService;
		this.objectMapper = objectMapper;
	}

	@JmsListener(destination = "#{@messagingProperties.transactionQueue()}")
	public void onMessage(Message message) throws Exception {
		bankAccountService.processTransaction(readTransaction(message));
	}

	private Transaction readTransaction(Message message) throws Exception {
		if (message instanceof TextMessage textMessage) {
			return objectMapper.readValue(textMessage.getText(), Transaction.class);
		}

		LOGGER.warn("Received unsupported message type {}", message.getClass().getName());
		throw new JMSException("Unsupported message type: " + message.getClass().getName());
	}
}
