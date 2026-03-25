# Technical Exercise - Bank Account - Extended

## Problem Statement

We need to implement a solution that is able to track the balance of a bank account supporting credits, debits and
balance enquiries. This system has an
audit requirement where the last 1000 transactions need to be sent to a downstream audit system. This audit system
requires transactions to be sent in
batches however the total value of transactions in each batch must not exceed £1,000,000. We are charged for each batch
sent therefore to save costs
must minimize the number of batches sent. The expectation is that the solution will have the following key applications:

* Producer application which is responsible for generating transactions
* Balance Tracker application which is responsible for:
    * processing the transactions
    * tracking the balance resulting from the transactions
    * publishing batches of balances to an audit system

## Requirements

### General

1. Your application should make use of Java and Maven
2. Your implementation is free to use libraries however an assessment should be made on the credibility of the library
3. Your solution should have appropriate and sufficient automated tests which validate requirements have been met.
4. Consideration should be given in the design of the two applications for how they can be released independently whilst
   ensuring the two applications remain compatible.
5. AI tooling (e.g, ChatGPT, CoPilot, Codeium) can be used in the completion of this exercise however anything produced
   by AI should be well understood

### Producer Application

1. The Producer application must be a separate JVM to the Balance Tracker
2. The logic to produce transactions should randomly generate a transaction that has an ID and an Amount. Transactions
   should be a mixture of credits and debits with values between £200 and £500,000.
3. An appropriate domain object should be created and called Transaction. Debits will have a negative amount and credits
   a positive amount.
4. The transaction ID can be any randomly generated value.
5. The production of the transactions should be performed on two separate dedicated threads - one for credits and one
   for debits
6. You can assume that all transactions being produced are for the same account therefore an account ID is not required.
7. You should produce credits and debits at a rate of 50 per second. i.e. 25 debits per second and 25 credits per second
8. Transactions that are generated should be published to the Balance Tracker application. An appropriate messaging
   technology should be implemented but consideration should be given to performance and resiliency.

### Balance Tracker Application

#### Position Tracking

1. The Balance Tracker application must be a separate JVM to the Producer
2. The Balance Tracker application should consume transactions from the Producer application using the selected
   messaging technology.
3. Your solution should define and implement the following interface (this can be extended if deemed necessary):
   ```java
   /**
   * Service to aggregate transactions tracking the overall balance for an account.
   */
   public interface BankAccountService {
     /**
     * Process a given transaction - this is to be called by the credit and debit generation threads.
     *
     * @param transaction transaction to process
     */
     void processTransaction(Transaction transaction);
     /**
     * Retrieve the balance in the account
     */
     double retrieveBalance();
   }
   ```
4. You can assume that all transactions being produced are for the same account therefore only a single instance of the
   above class is required
5. The functionality to retrieve the balance should be exposed over an appropriate REST API.

#### Audit

1. The balance tracker must create submissions for publication to a mocked audit system
2. Submissions to the audit system must contain exactly 1000 transactions
3. Each submission to the audit system should be made up of 1 or more batches. However, there is a charge for each batch
   in a submission therefore the number of batches in a submissions should be minimised
4. The absolute value of the transactions being submitted in each batch must not exceed £1,000,000. Credits do not
   offset debits. For example, if you have a debit and a credit transaction for £10 the total value in the batch is £20,
   not £0.
5. The audit system can be mocked where each time a submission occurs you can simply print to the console the details of
   the batches. For example:
   ```
   {
      submission: {
         batches: [
            {
               totalValueOfAllTransactions: 75000
               countOfTransactions: 18
            },
            {
               totalValueOfAllTransactions: 98000
               countOfTransactions: 12
            },
            ...
         ]
      }
   }
   ```
6. You should consider the performance of your implementation and demonstrate how the implementation would scale should
   the number of transactions in a submission be increased (i.e. submissions triggered every 100,000 transactions)
