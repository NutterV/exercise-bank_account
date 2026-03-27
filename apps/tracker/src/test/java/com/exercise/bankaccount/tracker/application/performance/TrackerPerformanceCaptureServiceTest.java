package com.exercise.bankaccount.tracker.application.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.exercise.bankaccount.tracker.application.config.TrackerPerformanceProperties;
import com.exercise.bankaccount.tracker.application.config.TrackerSubmissionProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TrackerPerformanceCaptureServiceTest {
	@Test
	void shouldCaptureConsumedProcessedAndPublishedMilestonesForEachSubmission() {
		TrackerPerformanceCaptureService captureService = trackerPerformanceCaptureService(true, 2);

		captureService.startCapture(4, 2);

		captureService.recordTransactionConsumed();
		captureService.recordTransactionProcessed();
		captureService.recordTransactionConsumed();
		captureService.recordTransactionProcessed();
		captureService.recordBatchingStarted();
		captureService.recordBatchingCompleted(1);
		captureService.recordSubmissionPublished();

		captureService.recordTransactionConsumed();
		captureService.recordTransactionProcessed();
		captureService.recordTransactionConsumed();
		captureService.recordTransactionProcessed();
		captureService.recordBatchingStarted();
		captureService.recordBatchingCompleted(2);
		captureService.recordSubmissionPublished();

		PerformanceCaptureSnapshot snapshot = captureService.stopCapture();

		assertTrue(snapshot.enabled());
		assertFalse(snapshot.active());
		assertTrue(snapshot.captureComplete());
		assertEquals(4, snapshot.processedTransactionCount());
		assertEquals(2, snapshot.publishedSubmissionCount());
		assertTrue(snapshot.firstTransactionConsumedAtNanos() > 0L);
		assertTrue(snapshot.firstTransactionProcessedAtNanos() > 0L);
		assertTrue(snapshot.lastTransactionProcessedAtNanos() >= snapshot.firstTransactionProcessedAtNanos());
		assertTrue(snapshot.lastSubmissionPublishedAtNanos() >= snapshot.firstTransactionConsumedAtNanos());
		assertEquals(2, snapshot.submissions().size());

		SubmissionPerformanceSnapshot firstSubmission = snapshot.submissions().get(0);
		assertEquals(1, firstSubmission.submissionIndex());
		assertEquals(1, firstSubmission.publishedBatchCount());
		assertTrue(firstSubmission.firstTransactionConsumedAtNanos() > 0L);
		assertTrue(firstSubmission.sealedAtNanos() >= firstSubmission.firstTransactionConsumedAtNanos());
		assertTrue(firstSubmission.batchingStartedAtNanos() >= firstSubmission.sealedAtNanos());
		assertTrue(firstSubmission.batchingCompletedAtNanos() >= firstSubmission.batchingStartedAtNanos());
		assertTrue(firstSubmission.publishedAtNanos() >= firstSubmission.batchingCompletedAtNanos());

		SubmissionPerformanceSnapshot secondSubmission = snapshot.submissions().get(1);
		assertEquals(2, secondSubmission.submissionIndex());
		assertEquals(2, secondSubmission.publishedBatchCount());
		assertTrue(secondSubmission.firstTransactionConsumedAtNanos() >= firstSubmission.publishedAtNanos());
		assertTrue(secondSubmission.publishedAtNanos() >= secondSubmission.batchingCompletedAtNanos());
	}

	@Test
	void shouldReturnDisabledSnapshotWhenInstrumentationIsOff() {
		TrackerPerformanceCaptureService captureService = trackerPerformanceCaptureService(false, 1_000);

		PerformanceCaptureSnapshot snapshot = captureService.startCapture(1_000, 1);

		assertFalse(snapshot.enabled());
		assertFalse(snapshot.active());
		assertEquals(0, snapshot.submissions().size());
	}

	private static TrackerPerformanceCaptureService trackerPerformanceCaptureService(boolean enabled, int submissionSize) {
		return new TrackerPerformanceCaptureService(new TrackerPerformanceProperties(enabled),
				new TrackerSubmissionProperties(submissionSize, 2, 10, BigDecimal.valueOf(1_000_000)));
	}
}
