package com.exercise.bankaccount.producer.application;

import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.commonservice.config.MessagingProperties;
import com.exercise.bankaccount.commonservice.messaging.JmsPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionPublisher extends JmsPublisher<Transaction> {
	public TransactionPublisher(
		JmsTemplate jmsTemplate, MessagingProperties messagingProperties, ObjectMapper objectMapper
	) {
		super(jmsTemplate, messagingProperties, objectMapper);
	}
}
