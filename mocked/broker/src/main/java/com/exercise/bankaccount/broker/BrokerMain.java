package com.exercise.bankaccount.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BrokerMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerMain.class);

    private BrokerMain() {
    }

    public static void main(String[] args) {
        LOGGER.info("Embedded broker stub starting");
    }
}
