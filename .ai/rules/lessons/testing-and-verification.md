# Lessons: Testing And Verification

- Trigger: Implementing or changing balance-tracking logic.
  Rule: Add deterministic tests that prove credits and debits update the balance exactly as expected.
  Expected validation: Tests cover representative transaction sequences and assert the resulting balance directly.

- Trigger: Implementing or changing audit submission behavior.
  Rule: Test the hard exercise constraints explicitly: exactly 1000 transactions per submission, batch totals based on absolute transaction values, and the threshold cap per batch.
  Expected validation: Tests fail on off-by-one submission counts, threshold breaches, or incorrect handling of debit versus credit signs.

- Trigger: Fixing a bug in batching, threshold handling, or transaction counting.
  Rule: Add a focused regression test that reproduces the prior failure with realistic values.
  Expected validation: The test would fail before the fix and passes after it.

- Trigger: Touching logic that depends on multiple threads or asynchronous processing.
  Rule: Verify concurrency-sensitive behavior with tests or controlled harnesses instead of assuming single-threaded correctness will carry over.
  Expected validation: Tests or targeted verification demonstrate no lost updates or inconsistent derived results under concurrent flow.

- Trigger: Iterating on a narrow change.
  Rule: Run the smallest meaningful test command first, then follow with a broader Maven verification command before declaring the task complete.
  Expected validation: Iteration stays fast while final validation still covers the changed surface area.
