package com.exercise.bankaccount.producer.application;

import com.exercise.bankaccount.commonservice.config.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class TransactionGenerationService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionGenerationService.class);

    private final ProducerProperties producerProperties;
    private final MessagingProperties messagingProperties;

    public TransactionGenerationService(
            ProducerProperties producerProperties,
            MessagingProperties messagingProperties
    ) {
        this.producerProperties = producerProperties;
        this.messagingProperties = messagingProperties;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        LOGGER.info(
                "Producer stub started with credit rate {} / debit rate {} targeting queue {}",
                producerProperties.creditsPerSecond(),
                producerProperties.debitsPerSecond(),
                messagingProperties.transactionQueue()
        );
    }
}
