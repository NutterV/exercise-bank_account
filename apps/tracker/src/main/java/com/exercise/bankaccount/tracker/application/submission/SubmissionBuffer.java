package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Fixed-size transaction window that becomes sealed once it reaches its
 * configured capacity.
 */
public final class SubmissionBuffer {
	private final int id;
	private final AtomicReferenceArray<Transaction> transactions;
	private final AtomicInteger size = new AtomicInteger();
	private final AtomicBoolean dispatched = new AtomicBoolean();

	/**
	 * @param id
	 *            stable identifier used only for diagnostics
	 * @param capacity
	 *            maximum number of transactions the buffer can hold before sealing
	 */
	public SubmissionBuffer(int id, int capacity) {
		this.id = id;
		this.transactions = new AtomicReferenceArray<>(capacity);
	}

	/**
	 * @return diagnostic buffer identifier
	 */
	public int id() {
		return id;
	}

	/**
	 * Attempts to append a transaction into the next free slot.
	 *
	 * @param transaction
	 *            transaction to append
	 * @return append status indicating whether the buffer stayed open, just sealed,
	 *         or was already full
	 */
	public AppendResult tryAppend(Transaction transaction) {
		int slot = size.getAndIncrement();
		if (slot >= transactions.length()) {
			size.decrementAndGet();
			return AppendResult.FULL;
		}

		transactions.set(slot, transaction);
		return slot + 1 == transactions.length() ? AppendResult.SEALED : AppendResult.APPENDED;
	}

	/**
	 * Marks this buffer as already scheduled so multiple threads do not dispatch it
	 * twice.
	 *
	 * @return {@code true} when this caller won dispatch ownership
	 */
	public boolean markDispatched() {
		return dispatched.compareAndSet(false, true);
	}

	/**
	 * Copies the sealed buffer contents into an immutable submission view for
	 * downstream processing.
	 *
	 * @return immutable list of the buffered transactions
	 */
	public List<Transaction> toList() {
		int transactionCount = Math.min(size.get(), transactions.length());
		List<Transaction> snapshot = new ArrayList<>(transactionCount);
		for (int index = 0; index < transactionCount; index++) {
			Transaction transaction = transactions.get(index);
			if (transaction == null) {
				throw new IllegalStateException("Submission buffer snapshot contains an unpublished transaction at slot "
						+ index + " for buffer " + id);
			}
			snapshot.add(transaction);
		}
		return List.copyOf(snapshot);
	}

	/**
	 * Clears buffer state so the instance can be returned to the pool.
	 */
	public void reset() {
		for (int index = 0; index < transactions.length(); index++) {
			transactions.set(index, null);
		}
		size.set(0);
		dispatched.set(false);
	}
}
