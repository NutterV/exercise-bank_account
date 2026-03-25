package com.exercise.bankaccount.tracker.application.submission;

/**
 * Result of attempting to append a transaction into a submission buffer.
 */
public enum AppendResult {
	APPENDED, SEALED, FULL
}
