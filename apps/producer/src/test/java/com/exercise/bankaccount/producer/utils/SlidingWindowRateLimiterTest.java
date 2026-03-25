package com.exercise.bankaccount.producer.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlidingWindowRateLimiterTest {

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
}
