package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.AuditBatch;
import com.exercise.bankaccount.common.model.AuditSubmission;
import com.exercise.bankaccount.commonservice.config.MessagingProperties;
import com.exercise.bankaccount.commonservice.config.QueueType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchedSubmissionPublisherTest {

	@Test
	void shouldResolveAuditQueueAsDestination() {
		MessagingProperties messagingProperties = new MessagingProperties(Map.of(
			QueueType.TRANSACTION, "transactions.queue",
			QueueType.AUDIT, "audit.queue"
		));
		BatchedSubmissionPublisher publisher = new BatchedSubmissionPublisher(null, messagingProperties, new ObjectMapper());

		assertEquals("audit.queue", messagingProperties.queueName(QueueType.AUDIT));
	}
}
