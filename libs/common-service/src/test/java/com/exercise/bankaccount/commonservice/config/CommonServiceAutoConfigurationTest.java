package com.exercise.bankaccount.commonservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

class CommonServiceAutoConfigurationTest {
	@Test
	void shouldCreateObjectMapperWithTimestampSerializationDisabled() {
		ObjectMapper objectMapper = new CommonServiceAutoConfiguration().objectMapper();

		assertThat(objectMapper).isNotNull();
		assertThat(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
	}
}
