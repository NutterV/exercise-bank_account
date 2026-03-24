package com.exercise.bankaccount.audit;

/**
 * Boots the mocked audit application and begins consuming audit submissions from the broker.
 */
public final class AuditMain {

    private AuditMain() {
    }

    /**
     * Starts the audit listener using runtime properties resolved from system properties or environment variables.
     *
     * @param args unused command-line arguments
     * @throws Exception if broker connection or message processing fails
     */
    public static void main(String[] args) throws Exception {
        AuditRuntimeProperties properties = AuditRuntimeProperties.fromSystem();
        AuditSubmissionListener listener = new AuditSubmissionListener(new AuditConsoleFormatter());
        Runtime.getRuntime().addShutdownHook(new Thread(listener::close, "audit-shutdown"));
        listener.start(properties);
        listener.awaitShutdown();
    }
}
