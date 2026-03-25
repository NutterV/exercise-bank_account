# Bank Account Exercise

## What This Solution Does

This repository contains a small event-driven bank-account system split across separate JVMs:

- `producer`
  Generates credit and debit transactions on two dedicated threads and publishes them to Artemis.
- `tracker`
  Consumes transactions, maintains the live account balance, exposes a REST endpoint for balance lookups, and builds audit submissions every 1000 transactions.
- `broker`
  A mocked embedded ActiveMQ Artemis broker used for local development.
- `audit`
  A mocked downstream audit consumer that prints received audit submissions to the console.

## High-Level Flow

1. `producer` creates credit and debit `Transaction` messages.
2. Messages are published to the transaction queue on Artemis.
3. `tracker` consumes each transaction and updates the running balance.
4. `tracker` appends transactions into rotating in-memory submission buffers.
5. Every sealed 1000-transaction window is handed to the background batcher.
6. The batcher groups transactions into `AuditBatch` items where each batch total stays at or below `1,000,000`.
7. The resulting `AuditSubmission` is published to the audit queue.
8. `audit` consumes the submission and prints it as formatted JSON.

## Runtime Components

### Producer

Location: [`apps/producer`](C:/git/exercise-bank_account/apps/producer)

Key behavior:

- runs two dedicated workers, one for credits and one for debits
- defaults to `25` credits/sec and `25` debits/sec
- generates values between `200` and `500000`
- publishes to the transaction queue through the shared JMS publisher

### Tracker

Location: [`apps/tracker`](C:/git/exercise-bank_account/apps/tracker)

Key behavior:

- listens to the transaction queue
- updates the live balance with a low-contention accumulator
- exposes `GET /balances/current`
- rotates across submission buffers so the hot transaction path stays simple
- seals a buffer every `1000` transactions
- grows the buffer pool from `2` up to `10` if processing falls behind
- batches submissions asynchronously before publishing to the audit queue

### Mocked Broker

Location: [`apps/mocked/broker`](C:/git/exercise-bank_account/apps/mocked/broker)

Key behavior:

- starts an embedded Artemis broker
- exposes a TCP acceptor on `tcp://127.0.0.1:61616` by default
- also exposes an in-vm acceptor for same-process use

### Mocked Audit

Location: [`apps/mocked/audit`](C:/git/exercise-bank_account/apps/mocked/audit)

Key behavior:

- listens to the audit queue
- deserializes `AuditSubmission`
- prints the submission in the mocked downstream format

## Requirements

- Java `21`
- Maven `3.9+` recommended

## How To Start Everything

Start the services in this order from the repo root:

1. Start the mocked broker:

```powershell
mvn -pl apps/mocked/broker -am -DskipTests exec:java "-Dexec.mainClass=com.exercise.bankaccount.broker.BrokerMain"
```

2. Start the tracker:

```powershell
mvn -pl apps/tracker -am spring-boot:run
```

3. Start the mocked audit consumer:

```powershell
mvn -pl apps/mocked/audit -am -DskipTests exec:java "-Dexec.mainClass=com.exercise.bankaccount.audit.AuditMain"
```

4. Start the producer:

```powershell
mvn -pl apps/producer -am spring-boot:run
```

Once all four are running:

- the producer will continuously publish transactions
- the tracker will maintain the live balance
- the audit app will print submissions as they arrive

## Querying The Balance

The tracker REST API is:

```text
GET http://localhost:8080/balances/current
```

Example:

```powershell
curl http://localhost:8080/balances/current
```

## Configuration

### Spring Application Config

Producer config file:
[`apps/producer/src/main/resources/application.yml`](C:/git/exercise-bank_account/apps/producer/src/main/resources/application.yml)

Tracker config file:
[`apps/tracker/src/main/resources/application.yml`](C:/git/exercise-bank_account/apps/tracker/src/main/resources/application.yml)

Producer keys:

- `spring.artemis.broker-url`
- `bank-account.producer.credits-per-second`
- `bank-account.producer.debits-per-second`
- `bank-account.producer.minimum-amount`
- `bank-account.producer.maximum-amount`

Tracker keys:

- `bank-account.messaging.queues.transaction`
- `bank-account.messaging.queues.audit`
- `bank-account.tracker.submission.submission-size`
- `bank-account.tracker.submission.initial-buffer-count`
- `bank-account.tracker.submission.max-buffer-count`
- `bank-account.tracker.submission.max-batch-total`

Shared queue defaults come from [`MessagingProperties.java`](C:/git/exercise-bank_account/libs/common-service/src/main/java/com/exercise/bankaccount/commonservice/config/MessagingProperties.java):

- `transaction` -> `bank-account.transactions`
- `audit` -> `bank-account.audit`

### Mocked Broker Config

Configured through JVM system properties or environment variables in
[`BrokerRuntimeProperties.java`](C:/git/exercise-bank_account/apps/mocked/broker/src/main/java/com/exercise/bankaccount/broker/BrokerRuntimeProperties.java).

System properties:

- `bank.account.broker.host`
- `bank.account.broker.port`
- `bank.account.broker.vm-id`

Environment variables:

- `BANK_ACCOUNT_BROKER_HOST`
- `BANK_ACCOUNT_BROKER_PORT`
- `BANK_ACCOUNT_BROKER_VM_ID`

Defaults:

- host: `127.0.0.1`
- port: `61616`
- vm id: `0`

Example:

```powershell
mvn -pl apps/mocked/broker -am -DskipTests exec:java "-Dexec.mainClass=com.exercise.bankaccount.broker.BrokerMain" "-Dexec.jvmArgs=-Dbank.account.broker.port=61617"
```

### Mocked Audit Config

Configured through JVM system properties or environment variables in
[`AuditRuntimeProperties.java`](C:/git/exercise-bank_account/apps/mocked/audit/src/main/java/com/exercise/bankaccount/audit/AuditRuntimeProperties.java).

System properties:

- `bank.account.audit.broker-url`
- `bank.account.audit.queue`

Environment variables:

- `BANK_ACCOUNT_AUDIT_BROKER_URL`
- `BANK_ACCOUNT_AUDIT_QUEUE`

Defaults:

- broker URL: `tcp://127.0.0.1:61616`
- queue: `bank-account.audit`

## Development Commands

Run the aggregate unit-test coverage report:

```powershell
mvn -pl coverage -am test
```

Report output:
[`coverage/target/site/jacoco-aggregate/index.html`](C:/git/exercise-bank_account/coverage/target/site/jacoco-aggregate/index.html)

Apply formatting:

```powershell
mvn spotless:apply
```

Check formatting:

```powershell
mvn verify -DskipTests
```

## Notes

- The tracker hot path currently routes audit batching through the greedy first-fit-decreasing planner in [`FirstFitDecreasingSubmissionBatchPlanner.java`](C:/git/exercise-bank_account/apps/tracker/src/main/java/com/exercise/bankaccount/tracker/application/submission/FirstFitDecreasingSubmissionBatchPlanner.java). This is the production algorithm because it has predictable performance.
- An exact branch-and-bound alternative is also kept in the codebase at [`ExactSubmissionBatchPlanner.java`](C:/git/exercise-bank_account/apps/tracker/src/main/java/com/exercise/bankaccount/tracker/application/submission/ExactSubmissionBatchPlanner.java). It is not used in production; it exists as a correctness reference, for comparative tests, and to support future bounded-optimal work.
- The tradeoff between the two planners is documented by [`SubmissionBatchPlannerPerformanceTest.java`](C:/git/exercise-bank_account/apps/tracker/src/test/java/com/exercise/bankaccount/tracker/application/submission/SubmissionBatchPlannerPerformanceTest.java), which shows where exact packing benefits disappear relative to runtime cost.
- Coverage is aggregated at repo level through the [`coverage`](C:/git/exercise-bank_account/coverage) module.
- Coverage excludes application/bootstrap/config/property wiring classes so the report focuses on executable logic.
