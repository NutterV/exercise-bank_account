package com.exercise.bankaccount.tracker.application.performance;

/**
 * Snapshot of one tracker performance capture session.
 *
 * @param enabled
 *            whether performance capture is enabled
 * @param active
 *            whether a capture is currently active
 * @param expectedTransactionCount
 *            configured transaction target
 * @param expectedSubmissionCount
 *            configured submission target
 * @param processedTransactionCount
 *            number of processed transactions seen so far
 * @param publishedSubmissionCount
 *            number of published audit submissions seen so far
 * @param startedAtNanos
 *            timestamp when capture started
 * @param firstTransactionConsumedAtNanos
 *            timestamp of the first transaction consumed from JMS
 * @param firstTransactionProcessedAtNanos
 *            timestamp of the first fully processed transaction
 * @param lastTransactionProcessedAtNanos
 *            timestamp of the latest processed transaction
 * @param firstSubmissionPublishedAtNanos
 *            timestamp of the first published audit submission
 * @param lastSubmissionPublishedAtNanos
 *            timestamp of the latest published audit submission
 * @param stoppedAtNanos
 *            timestamp when capture was stopped
 */
public record PerformanceCaptureSnapshot(
    boolean enabled, boolean active, int expectedTransactionCount,
		int expectedSubmissionCount, int processedTransactionCount, int publishedSubmissionCount, long startedAtNanos,
		long firstTransactionConsumedAtNanos, long firstTransactionProcessedAtNanos, long lastTransactionProcessedAtNanos,
		long firstSubmissionPublishedAtNanos, long lastSubmissionPublishedAtNanos, long stoppedAtNanos,
		java.util.List<SubmissionPerformanceSnapshot> submissions
) {
	/**
	 * @return whether the configured transaction target has been reached
	 */
	public boolean transactionTargetReached() {
		return processedTransactionCount >= expectedTransactionCount;
	}

	/**
	 * @return whether the configured submission target has been reached
	 */
	public boolean submissionTargetReached() {
		return publishedSubmissionCount >= expectedSubmissionCount;
	}

	/**
	 * @return whether both configured targets have been reached
	 */
	public boolean captureComplete() {
		return transactionTargetReached() && submissionTargetReached();
	}
}
