package com.exercise.bankaccount.tracker.application.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime settings for tracker-side submission buffering and pool growth.
 *
 * @param submissionSize
 *            number of transactions collected before a submission window seals
 * @param initialBufferCount
 *            number of submission buffers allocated eagerly at startup
 * @param maxBufferCount
 *            upper bound on dynamic buffer-pool growth under backlog
 * @param maxBatchTotal
 *            maximum absolute value allowed in one audit batch
 */
@ConfigurationProperties(prefix = "bank-account.tracker.submission")
public record TrackerSubmissionProperties(
    int submissionSize, int initialBufferCount, int maxBufferCount, BigDecimal maxBatchTotal
) {
	private static final int DEFAULT_SUBMISSION_SIZE = 1_000;
	private static final int DEFAULT_INITIAL_BUFFER_COUNT = 2;
	private static final int DEFAULT_MAX_BUFFER_COUNT = 10;
	private static final BigDecimal DEFAULT_MAX_BATCH_TOTAL = BigDecimal.valueOf(1_000_000);

	/**
	 * Applies defaults and guards against invalid pool sizing.
	 */
	public TrackerSubmissionProperties {
		submissionSize = submissionSize <= 0 ? DEFAULT_SUBMISSION_SIZE : submissionSize;
		initialBufferCount = initialBufferCount <= 0 ? DEFAULT_INITIAL_BUFFER_COUNT : initialBufferCount;
		maxBufferCount = maxBufferCount <= 0 ? DEFAULT_MAX_BUFFER_COUNT : maxBufferCount;
		maxBatchTotal = maxBatchTotal == null || maxBatchTotal.signum() <= 0 ? DEFAULT_MAX_BATCH_TOTAL : maxBatchTotal;

		if (initialBufferCount > maxBufferCount) {
			throw new IllegalArgumentException("initialBufferCount must not exceed maxBufferCount");
		}
	}
}
