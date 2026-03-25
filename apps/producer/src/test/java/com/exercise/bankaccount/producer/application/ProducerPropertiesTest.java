package com.exercise.bankaccount.producer.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProducerPropertiesTest {
	@Test
	void shouldApplyDefaultsWhenValuesAreMissingOrInvalid() {
		ProducerProperties properties = new ProducerProperties(0, -1, null, null);

		assertThat(properties.creditsPerSecond()).isEqualTo(25);
		assertThat(properties.debitsPerSecond()).isEqualTo(25);
		assertThat(properties.minimumAmount()).isEqualByComparingTo("200");
		assertThat(properties.maximumAmount()).isEqualByComparingTo("500000");
	}

	@Test
	void shouldResetMaximumAmountWhenBelowMinimum() {
		ProducerProperties properties = new ProducerProperties(10, 20, BigDecimal.valueOf(300), BigDecimal.valueOf(50));

		assertThat(properties.minimumAmount()).isEqualByComparingTo("300");
		assertThat(properties.maximumAmount()).isEqualByComparingTo("500000");
	}

	@Test
	void shouldKeepExplicitValidValues() {
		ProducerProperties properties = new ProducerProperties(10, 20, BigDecimal.valueOf(300), BigDecimal.valueOf(900));

		assertThat(properties.creditsPerSecond()).isEqualTo(10);
		assertThat(properties.debitsPerSecond()).isEqualTo(20);
		assertThat(properties.minimumAmount()).isEqualByComparingTo("300");
		assertThat(properties.maximumAmount()).isEqualByComparingTo("900");
	}
}
