package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.AuditSubmission;
import com.exercise.bankaccount.commonservice.config.MessagingProperties;
import com.exercise.bankaccount.commonservice.config.QueueType;
import com.exercise.bankaccount.commonservice.messaging.JmsPublisher;
import com.exercise.bankaccount.tracker.application.performance.TrackerPerformanceCaptureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes batched audit submissions to the audit queue.
 */
@Component
public class BatchedSubmissionPublisher extends JmsPublisher<AuditSubmission> {
	private final TrackerPerformanceCaptureService trackerPerformanceCaptureService;

	/**
	 * @param jmsTemplate
	 *            JMS template used to send audit submissions
	 * @param messagingProperties
	 *            configured broker destinations
	 * @param objectMapper
	 *            mapper used to serialize audit submissions
	 */
	public BatchedSubmissionPublisher(JmsTemplate jmsTemplate, MessagingProperties messagingProperties,
			ObjectMapper objectMapper, TrackerPerformanceCaptureService trackerPerformanceCaptureService) {
		super(jmsTemplate, messagingProperties, objectMapper, QueueType.AUDIT);
		this.trackerPerformanceCaptureService = trackerPerformanceCaptureService;
	}

	@Override
	public void publish(AuditSubmission auditSubmission) throws Exception {
		super.publish(auditSubmission);
		trackerPerformanceCaptureService.recordSubmissionPublished();
	}
}
