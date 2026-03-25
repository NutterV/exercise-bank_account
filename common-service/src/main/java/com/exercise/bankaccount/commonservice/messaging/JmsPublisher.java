package com.exercise.bankaccount.commonservice.messaging;

import com.exercise.bankaccount.commonservice.config.MessagingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jms.core.JmsTemplate;

/**
 * Serializes transactions as JSON text messages and publishes them to Artemis.
 */
public abstract class JmsPublisher<T> {
	private final JmsTemplate jmsTemplate;
	private final MessagingProperties messagingProperties;
	private final ObjectMapper objectMapper;

	public JmsPublisher(
		JmsTemplate jmsTemplate,
		MessagingProperties messagingProperties,
		ObjectMapper objectMapper
	) {
		this.jmsTemplate = jmsTemplate;
		this.messagingProperties = messagingProperties;
		this.objectMapper = objectMapper;
	}

	public void publish(T t) throws Exception {
		String payload = objectMapper.writeValueAsString(t);
		jmsTemplate.send(
			messagingProperties.transactionQueue(),
			session -> session.createTextMessage(payload)
		);
	}
}
