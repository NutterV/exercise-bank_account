package com.exercise.bankaccount.producer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

class SlidingWindowRateLimiterTest {
	@Test
	void shouldRejectZeroOrNegativeMaxRequests() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new SlidingWindowRateLimiter(0, 1_000L));

		assertEquals("maxRequests must be > 0", exception.getMessage());
	}

	@Test
	void shouldRejectZeroOrNegativeWindowMillis() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new SlidingWindowRateLimiter(2, 0L));

		assertEquals("windowMillis must be > 0", exception.getMessage());
	}

	@Test
	void shouldRejectRequestsWhenWindowIsFull() {
		SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, 1_000L);

		assertTrue(limiter.allow("credit", 1_000L));
		assertTrue(limiter.allow("credit", 1_100L));
		assertFalse(limiter.allow("credit", 1_200L));
	}

	@Test
	void shouldAllowRequestsAgainAfterWindowExpires() {
		SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, 1_000L);

		assertTrue(limiter.allow("credit", 1_000L));
		assertTrue(limiter.allow("credit", 1_100L));
		assertTrue(limiter.allow("credit", 2_001L));
	}

	@Test
	void shouldTrackKeysIndependently() {
		SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(1, 1_000L);

		assertTrue(limiter.allow("credit", 1_000L));
		assertTrue(limiter.allow("debit", 1_000L));
		assertFalse(limiter.allow("credit", 1_100L));
		assertFalse(limiter.allow("debit", 1_100L));
	}

	@Test
	void shouldEvictRequestsAtExactWindowBoundary() {
		SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(1, 1_000L);

		assertTrue(limiter.allow("credit", 1_000L));
		assertTrue(limiter.allow("credit", 2_000L));
	}

	@Test
	void shouldRetainActiveKeysDuringCleanup() {
		SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(1, 1_000L);

		assertTrue(limiter.allow("credit", 1_500L));

		limiter.cleanup(2_000L);

		assertFalse(limiter.allow("credit", 2_001L));
	}

	@Test
	void shouldRemoveExpiredKeysDuringCleanup() throws ReflectiveOperationException {
		SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(1, 1_000L);

		assertTrue(limiter.allow("credit", 1_000L));

		limiter.cleanup(2_000L);

		assertTrue(requestTracker(limiter).isEmpty());
		assertTrue(limiter.allow("credit", 2_001L));
	}

	@Test
	void shouldWrapRingBufferAcrossMultipleWindows() {
		SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, 100L);

		assertTrue(limiter.allow("credit", 100L));
		assertTrue(limiter.allow("credit", 150L));
		assertTrue(limiter.allow("credit", 201L));
		assertTrue(limiter.allow("credit", 252L));
		assertFalse(limiter.allow("credit", 253L));
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, Object> requestTracker(SlidingWindowRateLimiter limiter)
			throws ReflectiveOperationException {
		Field requestTrackerField = SlidingWindowRateLimiter.class.getDeclaredField("requestTracker");
		requestTrackerField.setAccessible(true);
		return (ConcurrentHashMap<String, Object>) requestTrackerField.get(limiter);
	}
}
