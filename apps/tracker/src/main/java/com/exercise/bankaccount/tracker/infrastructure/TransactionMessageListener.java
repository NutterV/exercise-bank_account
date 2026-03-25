package com.exercise.bankaccount.tracker.infrastructure;

import com.exercise.bankaccount.common.model.Transaction;
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
 * Consumes transaction messages from the broker and hands them to the tracker
 * service.
 */
@Component
public class TransactionMessageListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMessageListener.class);

	private final BankAccountService bankAccountService;
	private final ObjectMapper objectMapper;

	/**
	 * @param bankAccountService
	 *            tracker service that owns balance and submission-window processing
	 * @param objectMapper
	 *            JSON mapper used to deserialize transaction messages
	 */
	public TransactionMessageListener(BankAccountService bankAccountService, ObjectMapper objectMapper) {
		this.bankAccountService = bankAccountService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Consumes one broker message and forwards the decoded transaction into the
	 * tracker service.
	 *
	 * @param message
	 *            JMS message expected to contain a serialized {@link Transaction}
	 * @throws Exception
	 *             when the payload cannot be decoded or is not a supported message
	 *             type
	 */
	@JmsListener(destination = "${bank-account.messaging.queues.transaction}")
	public void onMessage(Message message) throws Exception {
		bankAccountService.processTransaction(readTransaction(message));
	}

	/**
	 * Deserializes a supported JMS message into a transaction payload.
	 *
	 * @param message
	 *            raw JMS message from the transaction queue
	 * @return decoded transaction
	 * @throws Exception
	 *             when the payload cannot be parsed or the message type is
	 *             unsupported
	 */
	private Transaction readTransaction(Message message) throws Exception {
		if (message instanceof TextMessage textMessage) {
			return objectMapper.readValue(textMessage.getText(), Transaction.class);
		}

		LOGGER.warn("Received unsupported message type {}", message.getClass().getName());
		throw new JMSException("Unsupported message type: " + message.getClass().getName());
	}
}
