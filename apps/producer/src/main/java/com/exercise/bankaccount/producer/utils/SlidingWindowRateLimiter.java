package com.exercise.bankaccount.producer.utils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limiter that tracks request timestamps per key and
 * decides whether another request is allowed within the configured time window.
 */
public class SlidingWindowRateLimiter {
	private final int maxRequests;
	private final long windowMillis;
	private final ConcurrentHashMap<String, RequestRate> requestTracker = new ConcurrentHashMap<>();

	/**
	 * Creates a rate limiter with a maximum number of requests allowed per key
	 * inside a rolling time window.
	 *
	 * @param maxRequests
	 *            maximum number of tracked requests allowed in the window for a
	 *            single key
	 * @param windowMillis
	 *            rolling window size in milliseconds
	 */
	public SlidingWindowRateLimiter(int maxRequests, long windowMillis) {
		if (maxRequests <= 0) {
			throw new IllegalArgumentException("maxRequests must be > 0");
		}
		if (windowMillis <= 0) {
			throw new IllegalArgumentException("windowMillis must be > 0");
		}
		this.maxRequests = maxRequests;
		this.windowMillis = windowMillis;
	}

	/**
	 * Attempts to record a request for the given key at the supplied time.
	 *
	 * @param key
	 *            logical rate-limiting key
	 * @param nowMillis
	 *            current timestamp in milliseconds
	 * @return {@code true} when the request is within the allowed rate, otherwise
	 *         {@code false}
	 */
	public boolean allow(String key, long nowMillis) {
		final RequestRate requestRate = requestTracker.computeIfAbsent(key, k -> new RequestRate(maxRequests));
		final long threshold = nowMillis - windowMillis;

		synchronized (requestRate) {
			requestRate.evictExpiredUnderLock(threshold);

			if (requestRate.trackedRequestCount == maxRequests) {
				return false;
			}
			requestRate.addUnderLock(nowMillis);

			return true;
		}
	}

	/**
	 * Removes expired request history and drops keys that no longer have any
	 * tracked requests.
	 *
	 * @param nowMillis
	 *            current timestamp in milliseconds
	 */
	public void cleanup(long nowMillis) {
		final long threshold = nowMillis - windowMillis;
		requestTracker.forEach((key, requestRate) -> {
			// noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (requestRate) {
				requestRate.evictExpiredUnderLock(threshold);
				if (requestRate.trackedRequestCount == 0) {
					requestTracker.remove(key, requestRate);
				}
			}
		});
	}

	/**
	 * Per-key ring buffer of request timestamps used while holding the enclosing
	 * monitor lock.
	 */
	private static final class RequestRate {
		private final long[] buffer;
		private int oldestIndex = 0;
		private int trackedRequestCount = 0;

		/**
		 * @param capacity
		 *            maximum number of timestamps that need to be tracked for the key
		 */
		RequestRate(int capacity) {
			this.buffer = new long[capacity];
		}

		/**
		 * Adds a request timestamp to the ring buffer while the caller already holds
		 * the lock.
		 *
		 * @param now
		 *            request timestamp in milliseconds
		 */
		void addUnderLock(long now) {
			final int nextWriteIndex = toBufferIndex(oldestIndex + trackedRequestCount);
			buffer[nextWriteIndex] = now;
			trackedRequestCount++;
		}

		/**
		 * Evicts request timestamps that have fallen outside the active sliding window.
		 *
		 * @param threshold
		 *            timestamps less than or equal to this value are considered expired
		 */
		void evictExpiredUnderLock(long threshold) {
			while (trackedRequestCount > 0) {
				final long oldest = buffer[oldestIndex];
				if (oldest > threshold) {
					break;
				}
				oldestIndex = toBufferIndex(oldestIndex + 1);
				trackedRequestCount--;
			}
		}

		/**
		 * Maps a logical ring-buffer position onto the backing array.
		 *
		 * @param logicalIndex
		 *            logical offset into the ring buffer
		 * @return physical array index
		 */
		private int toBufferIndex(int logicalIndex) {
			return logicalIndex % buffer.length;
		}
	}
}
