package com.exercise.bankaccount.broker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class BrokerMainTest {

    @Test
    void shouldStartStubBrokerMain() {
        assertDoesNotThrow(() -> BrokerMain.main(new String[0]));
    }
}
