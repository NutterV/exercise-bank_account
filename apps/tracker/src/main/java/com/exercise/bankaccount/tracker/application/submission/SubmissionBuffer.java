package com.exercise.bankaccount.tracker.application.submission;

import com.exercise.bankaccount.common.model.Transaction;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-size transaction window that becomes sealed once it reaches its configured capacity.
 */
public final class SubmissionBuffer {
	private final int id;
	private final Transaction[] transactions;
	private final AtomicInteger size = new AtomicInteger();
	private final AtomicBoolean dispatched = new AtomicBoolean();

	/**
	 * @param id       stable identifier used only for diagnostics
	 * @param capacity maximum number of transactions the buffer can hold before sealing
	 */
	public SubmissionBuffer(int id, int capacity) {
		this.id = id;
		this.transactions = new Transaction[capacity];
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
	 * @param transaction transaction to append
	 * @return append status indicating whether the buffer stayed open, just sealed, or was already full
	 */
	public AppendResult tryAppend(Transaction transaction) {
		int slot = size.getAndIncrement();
		if (slot >= transactions.length) {
			size.decrementAndGet();
			return AppendResult.FULL;
		}

		transactions[slot] = transaction;
		return slot + 1 == transactions.length ? AppendResult.SEALED : AppendResult.APPENDED;
	}

	/**
	 * Marks this buffer as already scheduled so multiple threads do not dispatch it twice.
	 *
	 * @return {@code true} when this caller won dispatch ownership
	 */
	public boolean markDispatched() {
		return dispatched.compareAndSet(false, true);
	}

	/**
	 * Copies the sealed buffer contents into an immutable submission view for downstream processing.
	 *
	 * @return immutable list of the buffered transactions
	 */
	public List<Transaction> toList() {
		return List.copyOf(Arrays.asList(Arrays.copyOf(transactions, size.get())));
	}

	/**
	 * Clears buffer state so the instance can be returned to the pool.
	 */
	public void reset() {
		Arrays.fill(transactions, null);
		size.set(0);
		dispatched.set(false);
	}
}
