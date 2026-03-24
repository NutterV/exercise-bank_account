package com.exercise.bankaccount.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuditMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditMain.class);

    private AuditMain() {
    }

    public static void main(String[] args) {
        LOGGER.info("Mocked audit stub starting");
    }
}
