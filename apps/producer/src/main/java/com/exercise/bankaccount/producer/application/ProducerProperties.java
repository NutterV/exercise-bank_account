package com.exercise.bankaccount.producer.application;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime settings for transaction generation and pacing.
 */
@ConfigurationProperties(prefix = "bank-account.producer")
public record ProducerProperties(int creditsPerSecond, int debitsPerSecond, BigDecimal minimumAmount,
		BigDecimal maximumAmount) {
	public ProducerProperties {
		creditsPerSecond = creditsPerSecond <= 0 ? 25 : creditsPerSecond;
		debitsPerSecond = debitsPerSecond <= 0 ? 25 : debitsPerSecond;
		minimumAmount = minimumAmount == null || minimumAmount.signum() <= 0 ? BigDecimal.valueOf(200) : minimumAmount;
		maximumAmount = maximumAmount == null || maximumAmount.compareTo(minimumAmount) < 0
				? BigDecimal.valueOf(500000)
				: maximumAmount;
	}
}
