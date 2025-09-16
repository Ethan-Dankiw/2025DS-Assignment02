package net.ethandankiw.data;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LamportClock {

	private static final Logger logger = LoggerFactory.getLogger(LamportClock.class);

	private final AtomicLong clock;


	/**
	 * Constructs a new LamportClock instance with a clock value of 0
	 */
	public LamportClock() {
		this.clock = new AtomicLong();
	}


	/**
	 * Returns the current value of the Lamport clock
	 *
	 * @return The current clock value
	 */
	public synchronized long getClockValue() {
		return clock.get();
	}


	/**
	 * Increments the clock by 1
	 */
	public synchronized void tick() {
		this.clock.incrementAndGet();
		logger.debug("Clock ticked to {}", this.clock);
	}

	/**
	 * Receives a clock value.
	 * The local clock is updated to the maximum of its current value and the
	 * received clock value + 1
	 *
	 * @param receivedClock The clock value received from a remote process.
	 */
	public synchronized void receive(long receivedClock) {
		// Update the lamport clock according to the received clock value
		this.updateClock(receivedClock);
		logger.debug("Clock received value {}, new clock is {}", receivedClock, getClockValue());
	}

	/**
	 * Private helper method to compare and update the clock
	 *
	 * @param received The clock value received from a remote process
	 */
	private synchronized void updateClock(long received) {
		// Calculate the updated clock value
		long value = this.compare(this.getClockValue(), received) + 1;

		// Update the local clock value
		this.updateClockValue(value);
	}

	/**
	 * Private helper method to get the maximum of two clock values
	 *
	 * @param local The local clock value
	 * @param received The received clock value
	 * @return The maximum of the two values
	 */
	private synchronized long compare(long local, long received) {
		return Math.max(local, received);
	}

	/**
	 * Private helper method to update the clock value atomically
	 *
	 * @param value The new clock value to set
	 */
	private synchronized void updateClockValue(long value) {
		this.clock.updateAndGet(current -> value);
	}
}
