package com.exercise.bankaccount.tracker.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feature flags for optional tracker performance instrumentation.
 *
 * @param enabled
 *            whether tracker-side performance capture should be active
 */
@ConfigurationProperties(prefix = "bank-account.tracker.performance")
public record TrackerPerformanceProperties(boolean enabled) {
}
