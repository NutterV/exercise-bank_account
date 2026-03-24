package com.exercise.bankaccount.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class AuditMainTest {

    @Test
    void shouldStartStubAuditMain() {
        assertDoesNotThrow(() -> AuditMain.main(new String[0]));
    }
}
