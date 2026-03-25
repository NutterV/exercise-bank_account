package com.exercise.bankaccount.audit;

import com.exercise.bankaccount.common.model.AuditSubmission;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Map;

/**
 * Formats audit submissions as pretty-printed JSON matching the exercise's
 * mocked audit output.
 */
final class AuditConsoleFormatter {
	private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
			.enable(SerializationFeature.INDENT_OUTPUT);

	static String format(AuditSubmission submission) throws Exception {
		return objectMapper.writeValueAsString(Map.of("submission", submission));
	}
}
