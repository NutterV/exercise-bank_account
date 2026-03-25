package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.Transaction;
import com.exercise.bankaccount.tracker.application.config.TrackerSubmissionProperties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Owns the active submission buffer, buffer-pool growth, and asynchronous
 * dispatch of sealed buffers.
 */
@Component
public class SubmissionBufferCoordinator {
	private final SubmissionProcessor submissionProcessor;
	private final LinkedBlockingQueue<SubmissionBuffer> availableBuffers = new LinkedBlockingQueue<>();
	private final AtomicReference<SubmissionBuffer> activeBuffer;
	private final AtomicInteger bufferCount;
	private final int submissionSize;
	private final int maxBufferCount;

	/**
	 * Creates the production coordinator using the configured submission and pool
	 * settings.
	 *
	 * @param submissionProcessor
	 *            background processor for completed submission windows
	 * @param trackerSubmissionProperties
	 *            configured submission-window and buffer-pool sizing
	 */
	public SubmissionBufferCoordinator(SubmissionProcessor submissionProcessor,
			TrackerSubmissionProperties trackerSubmissionProperties) {
		this(submissionProcessor, trackerSubmissionProperties.submissionSize(),
				trackerSubmissionProperties.initialBufferCount(), trackerSubmissionProperties.maxBufferCount());
	}

	/**
	 * Test-friendly constructor that allows the buffer-pool and executor strategy
	 * to be controlled explicitly.
	 *
	 * @param submissionProcessor
	 *            handler for completed submission windows
	 * @param submissionSize
	 *            number of transactions required to seal one submission buffer
	 * @param initialBufferCount
	 *            number of buffers available before the pool needs to grow
	 * @param maxBufferCount
	 *            upper bound for dynamic buffer-pool growth
	 */
	public SubmissionBufferCoordinator(SubmissionProcessor submissionProcessor, int submissionSize,
			int initialBufferCount, int maxBufferCount) {
		this.submissionProcessor = submissionProcessor;
		this.submissionSize = submissionSize;
		this.maxBufferCount = maxBufferCount;
		this.bufferCount = new AtomicInteger(initialBufferCount);

		for (int index = 0; index < initialBufferCount; index++) {
			availableBuffers.add(new SubmissionBuffer(index, submissionSize));
		}

		final SubmissionBuffer initialActiveBuffer = availableBuffers.poll();
		if (initialActiveBuffer == null) {
			throw new IllegalStateException("At least one submission buffer must be configured");
		}
		this.activeBuffer = new AtomicReference<>(initialActiveBuffer);
	}

	/**
	 * Appends a transaction to the current active submission buffer and rotates
	 * buffers when one seals.
	 *
	 * @param transaction
	 *            transaction to record in the current submission window
	 */
	public void record(Transaction transaction) {
		while (true) {
			final SubmissionBuffer currentBuffer = activeBuffer.get();
			final AppendResult result = currentBuffer.tryAppend(transaction);
			if (result == AppendResult.APPENDED) {
				return;
			}
			if (result == AppendResult.SEALED) {
				rotateAndDispatch(currentBuffer);
				return;
			}
			rotateIfNecessary(currentBuffer);
		}
	}

	/**
	 * Ensures the sealed buffer is no longer active and then schedules it for
	 * background processing.
	 *
	 * @param sealedBuffer
	 *            buffer that just reached the configured submission size
	 */
	private void rotateAndDispatch(SubmissionBuffer sealedBuffer) {
		rotateIfNecessary(sealedBuffer);
	}

	/**
	 * Swaps the current active buffer with a recyclable or newly created buffer.
	 *
	 * @param fullBuffer
	 *            buffer that can no longer accept transactions
	 */
	private void rotateIfNecessary(SubmissionBuffer fullBuffer) {
		while (activeBuffer.get() == fullBuffer) {
			SubmissionBuffer nextBuffer = acquireNextBuffer();
			if (activeBuffer.compareAndSet(fullBuffer, nextBuffer)) {
				dispatchIfNeeded(fullBuffer);
				return;
			}
			releaseBuffer(nextBuffer);
		}
	}

	/**
	 * Schedules one sealed buffer for background processing and returns it to the
	 * pool afterward.
	 *
	 * @param sealedBuffer
	 *            completed submission buffer
	 */
	private void dispatchIfNeeded(SubmissionBuffer sealedBuffer) {
		if (!sealedBuffer.markDispatched()) {
			return;
		}

		try {
			submissionProcessor.processSubmission(sealedBuffer.toList());
		} finally {
			sealedBuffer.reset();
			releaseBuffer(sealedBuffer);
		}
	}

	/**
	 * Gets the next writable buffer, preferring recycled buffers and growing the
	 * pool only when needed.
	 *
	 * @return buffer ready to become the new active submission window
	 */
	private SubmissionBuffer acquireNextBuffer() {
		SubmissionBuffer recycledBuffer = availableBuffers.poll();
		if (recycledBuffer != null) {
			return recycledBuffer;
		}

		int currentCount = bufferCount.get();
		if (currentCount < maxBufferCount && bufferCount.compareAndSet(currentCount, currentCount + 1)) {
			return new SubmissionBuffer(currentCount, submissionSize);
		}

		try {
			return availableBuffers.take();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for a submission buffer", exception);
		}
	}

	/**
	 * Returns a reset buffer to the recyclable pool.
	 *
	 * @param buffer
	 *            buffer ready for reuse
	 */
	private void releaseBuffer(SubmissionBuffer buffer) {
		availableBuffers.offer(buffer);
	}
}
