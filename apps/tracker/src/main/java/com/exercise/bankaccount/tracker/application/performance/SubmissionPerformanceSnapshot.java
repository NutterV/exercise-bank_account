package com.exercise.bankaccount.tracker.application.performance;

/**
 * Per-submission performance milestones for one sealed submission window.
 *
 * @param submissionIndex
 *            one-based submission sequence within the active capture
 * @param firstTransactionConsumedAtNanos
 *            timestamp of the first consumed transaction in this submission
 * @param sealedAtNanos
 *            timestamp when the submission reached its configured size
 * @param batchingStartedAtNanos
 *            timestamp when batch planning started for this submission
 * @param batchingCompletedAtNanos
 *            timestamp when batch planning finished for this submission
 * @param publishedAtNanos
 *            timestamp when the audit submission was published
 * @param publishedBatchCount
 *            number of audit batches emitted for this submission
 */
public record SubmissionPerformanceSnapshot(
    int submissionIndex, long firstTransactionConsumedAtNanos, long sealedAtNanos,
    long batchingStartedAtNanos, long batchingCompletedAtNanos, long publishedAtNanos, int publishedBatchCount
) {
}
