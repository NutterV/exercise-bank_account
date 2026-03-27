package com.exercise.bankaccount.tracker.application.performance;

import com.exercise.bankaccount.tracker.application.config.TrackerPerformanceProperties;
import com.exercise.bankaccount.tracker.application.config.TrackerSubmissionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Feature-flagged runtime performance capture for tracker ingestion and audit
 * publication milestones.
 */
@Service
public class TrackerPerformanceCaptureService {
  private final boolean enabled;
  private final int submissionSize;
  private final AtomicReference<CaptureState> activeCapture = new AtomicReference<>();

  @Autowired
  public TrackerPerformanceCaptureService(
    TrackerPerformanceProperties trackerPerformanceProperties,
    TrackerSubmissionProperties trackerSubmissionProperties
  ) {
    this(trackerPerformanceProperties.enabled(), trackerSubmissionProperties.submissionSize());
  }

  private TrackerPerformanceCaptureService(boolean enabled, int submissionSize) {
    this.enabled = enabled;
    this.submissionSize = submissionSize;
  }

  /**
   * Creates a disabled capture service for focused tests that instantiate
   * collaborators outside the Spring container.
   *
   * @param submissionSize
   *            submission size used when deriving per-submission milestones
   * @return disabled capture service that records nothing
   */
  public static TrackerPerformanceCaptureService disabled(int submissionSize) {
    return new TrackerPerformanceCaptureService(false, submissionSize);
  }

  /**
   * Starts a fresh capture session for one controlled run.
   *
   * @param expectedTransactionCount
   *            number of transactions expected during the capture
   * @param expectedSubmissionCount
   *            number of submissions expected during the capture
   * @return snapshot of the newly started capture state
   */
  public PerformanceCaptureSnapshot startCapture(int expectedTransactionCount, int expectedSubmissionCount) {
    if (!enabled) {
      return disabledSnapshot();
    }

    CaptureState captureState = new CaptureState(expectedTransactionCount, expectedSubmissionCount, System.nanoTime());
    activeCapture.set(captureState);
    return captureState.snapshot(true, enabled);
  }

  /**
   * Returns the current snapshot without altering capture state.
   *
   * @return current capture snapshot, or an idle/disabled snapshot when capture
   *         is not active
   */
  public PerformanceCaptureSnapshot currentCapture() {
    if (!enabled) {
      return disabledSnapshot();
    }

    CaptureState captureState = activeCapture.get();
    return captureState == null ? idleSnapshot() : captureState.snapshot(true, enabled);
  }

  /**
   * Stops the active capture and returns its final snapshot.
   *
   * @return final snapshot for the just-stopped capture, or an idle/disabled
   *         snapshot when no capture is active
   */
  public PerformanceCaptureSnapshot stopCapture() {
    if (!enabled) {
      return disabledSnapshot();
    }

    CaptureState captureState = activeCapture.getAndSet(null);
    if (captureState == null) {
      return idleSnapshot();
    }

    captureState.stoppedAtNanos.compareAndSet(0L, System.nanoTime());
    return captureState.snapshot(false, enabled);
  }

  /**
   * Records that one transaction has completed tracker-side processing.
   */
  public void recordTransactionProcessed() {
    CaptureState captureState = activeCapture.get();
    if (captureState == null) {
      return;
    }

    long now = System.nanoTime();
    int processedTransactionNumber = captureState.processedTransactionCount.incrementAndGet();
    captureState.firstTransactionProcessedAtNanos.compareAndSet(0L, now);
    captureState.lastTransactionProcessedAtNanos.set(now);
    captureState.recordTransactionProcessedMilestone(processedTransactionNumber, submissionSize, now);
  }

  /**
   * Records that one transaction has been consumed from the JMS queue.
   */
  public void recordTransactionConsumed() {
    CaptureState captureState = activeCapture.get();
    if (captureState == null) {
      return;
    }

    long now = System.nanoTime();
    int consumedTransactionNumber = captureState.consumedTransactionCount.incrementAndGet();
    captureState.firstTransactionConsumedAtNanos.compareAndSet(0L, now);
    captureState.recordTransactionConsumedMilestone(consumedTransactionNumber, submissionSize, now);
  }

  /**
   * Records the start of batch planning for the next sealed submission window.
   */
  public void recordBatchingStarted() {
    CaptureState captureState = activeCapture.get();
    if (captureState == null) {
      return;
    }

    synchronized (captureState) {
      SubmissionMetricState submissionMetricState = captureState.nextSubmissionAwaitingBatchingStart();
      if (submissionMetricState != null) {
        submissionMetricState.markBatchingStarted();
      }
    }
  }

  /**
   * Records the completion of batch planning for the next in-flight submission
   * window.
   *
   * @param publishedBatchCount
   *            number of audit batches produced for the submission
   */
  public void recordBatchingCompleted(int publishedBatchCount) {
    CaptureState captureState = activeCapture.get();
    if (captureState == null) {
      return;
    }

    synchronized (captureState) {
      SubmissionMetricState submissionMetricState = captureState.nextSubmissionAwaitingBatchingCompletion();
      if (submissionMetricState != null) {
        submissionMetricState.markBatchingCompleted(publishedBatchCount);
      }
    }
  }

  /**
   * Records publication of the next fully batched submission to the audit
   * queue.
   */
  public void recordSubmissionPublished() {
    CaptureState captureState = activeCapture.get();
    if (captureState == null) {
      return;
    }

    synchronized (captureState) {
      SubmissionMetricState submissionMetricState = captureState.nextSubmissionAwaitingPublish();
      if (submissionMetricState == null) {
        return;
      }

      submissionMetricState.markPublished();
      captureState.firstSubmissionPublishedAtNanos.compareAndSet(0L, submissionMetricState.publishedAtNanos.get());
      captureState.lastSubmissionPublishedAtNanos.set(submissionMetricState.publishedAtNanos.get());
      captureState.publishedSubmissionCount.incrementAndGet();
    }
  }

  private PerformanceCaptureSnapshot disabledSnapshot() {
    return new PerformanceCaptureSnapshot(
      false,
      false,
      0,
      0,
      0,
      0,
      0L,
      0L,
      0L,
      0L,
      0L,
      0L,
      0L,
      List.of()
    );
  }

  private PerformanceCaptureSnapshot idleSnapshot() {
    return new PerformanceCaptureSnapshot(
      true,
      false,
      0,
      0,
      0,
      0,
      0L,
      0L,
      0L,
      0L,
      0L,
      0L,
      0L,
      List.of()
    );
  }

  private static final class CaptureState {
    private final int expectedTransactionCount;
    private final int expectedSubmissionCount;
    private final long startedAtNanos;
    private final AtomicInteger consumedTransactionCount = new AtomicInteger();
    private final AtomicInteger processedTransactionCount = new AtomicInteger();
    private final AtomicInteger publishedSubmissionCount = new AtomicInteger();
    private final AtomicLong firstTransactionConsumedAtNanos = new AtomicLong();
    private final AtomicLong firstTransactionProcessedAtNanos = new AtomicLong();
    private final AtomicLong lastTransactionProcessedAtNanos = new AtomicLong();
    private final AtomicLong firstSubmissionPublishedAtNanos = new AtomicLong();
    private final AtomicLong lastSubmissionPublishedAtNanos = new AtomicLong();
    private final AtomicLong stoppedAtNanos = new AtomicLong();
    private final List<SubmissionMetricState> submissions = new ArrayList<>();

    private CaptureState(int expectedTransactionCount, int expectedSubmissionCount, long startedAtNanos) {
      this.expectedTransactionCount = expectedTransactionCount;
      this.expectedSubmissionCount = expectedSubmissionCount;
      this.startedAtNanos = startedAtNanos;
    }

    private void recordTransactionConsumedMilestone(int consumedTransactionNumber, int submissionSize, long now) {
      synchronized (this) {
        if ((consumedTransactionNumber - 1) % submissionSize == 0) {
          submissions.add(new SubmissionMetricState(submissions.size() + 1, now));
        }
      }
    }

    private void recordTransactionProcessedMilestone(int processedTransactionNumber, int submissionSize, long now) {
      synchronized (this) {
        if (processedTransactionNumber % submissionSize == 0 && !submissions.isEmpty()) {
          submissions.getLast().sealedAtNanos.compareAndSet(0L, now);
        }
      }
    }

    private SubmissionMetricState nextSubmissionAwaitingBatchingStart() {
      for (SubmissionMetricState submission : submissions) {
        if (submission.sealedAtNanos.get() != 0L && submission.batchingStartedAtNanos.get() == 0L) {
          return submission;
        }
      }
      return null;
    }

    private SubmissionMetricState nextSubmissionAwaitingBatchingCompletion() {
      for (SubmissionMetricState submission : submissions) {
        if (submission.batchingStartedAtNanos.get() != 0L && submission.batchingCompletedAtNanos.get() == 0L) {
          return submission;
        }
      }
      return null;
    }

    private SubmissionMetricState nextSubmissionAwaitingPublish() {
      for (SubmissionMetricState submission : submissions) {
        if (submission.batchingCompletedAtNanos.get() != 0L && submission.publishedAtNanos.get() == 0L) {
          return submission;
        }
      }
      return null;
    }

    private PerformanceCaptureSnapshot snapshot(boolean active, boolean enabled) {
      return new PerformanceCaptureSnapshot(
        enabled, active, expectedTransactionCount, expectedSubmissionCount,
        processedTransactionCount.get(), publishedSubmissionCount.get(), startedAtNanos,
        firstTransactionConsumedAtNanos.get(),
        firstTransactionProcessedAtNanos.get(), lastTransactionProcessedAtNanos.get(),
        firstSubmissionPublishedAtNanos.get(), lastSubmissionPublishedAtNanos.get(), stoppedAtNanos.get(),
        submissions.stream().map(SubmissionMetricState::snapshot).toList()
      );
    }
  }

  private static final class SubmissionMetricState {
    private final int submissionIndex;
    private final long firstTransactionConsumedAtNanos;
    private final AtomicLong sealedAtNanos = new AtomicLong();
    private final AtomicLong batchingStartedAtNanos = new AtomicLong();
    private final AtomicLong batchingCompletedAtNanos = new AtomicLong();
    private final AtomicLong publishedAtNanos = new AtomicLong();
    private final AtomicInteger publishedBatchCount = new AtomicInteger();

    private SubmissionMetricState(int submissionIndex, long firstTransactionConsumedAtNanos) {
      this.submissionIndex = submissionIndex;
      this.firstTransactionConsumedAtNanos = firstTransactionConsumedAtNanos;
    }

    private void markBatchingStarted() {
      batchingStartedAtNanos.compareAndSet(0L, System.nanoTime());
    }

    private void markBatchingCompleted(int batchCount) {
      publishedBatchCount.set(batchCount);
      batchingCompletedAtNanos.compareAndSet(0L, System.nanoTime());
    }

    private void markPublished() {
      publishedAtNanos.compareAndSet(0L, System.nanoTime());
    }

    private SubmissionPerformanceSnapshot snapshot() {
      return new SubmissionPerformanceSnapshot(
        submissionIndex, firstTransactionConsumedAtNanos, sealedAtNanos.get(),
        batchingStartedAtNanos.get(), batchingCompletedAtNanos.get(), publishedAtNanos.get(),
        publishedBatchCount.get()
      );
    }
  }
}
