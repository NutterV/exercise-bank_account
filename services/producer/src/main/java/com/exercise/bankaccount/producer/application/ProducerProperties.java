package com.exercise.bankaccount.producer.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bank-account.producer")
public record ProducerProperties(int creditsPerSecond, int debitsPerSecond) {

    public ProducerProperties {
        creditsPerSecond = creditsPerSecond <= 0 ? 25 : creditsPerSecond;
        debitsPerSecond = debitsPerSecond <= 0 ? 25 : debitsPerSecond;
    }
}
