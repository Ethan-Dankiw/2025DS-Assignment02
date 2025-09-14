package net.ethandankiw.data;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LamportClock {
	private static final Logger logger = LoggerFactory.getLogger(LamportClock.class);

	private final AtomicLong clock;

	public LamportClock() {
		this.clock = new AtomicLong();
	}

	public long getClockValue() {
		return clock.get();
	}

	public void tick() {
		this.clock.incrementAndGet();
		logger.debug("Clock ticked to {}", this.clock);
	}

	public synchronized void receive(long receivedClock) {
		// Update the lamport clock according to the received clock value
		this.updateClock(receivedClock);
		logger.debug("Clock received value {}, new clock is {}", receivedClock, this.clock);
	}

	private synchronized void updateClock(long received) {
		// Calculate the updated clock value
		long value = this.compare(this.getClockValue(), received) + 1;

		// Update the local clock value
		this.updateClockValue(value);
	}

	private synchronized long compare(long local, long received) {
		return Math.max(local, received);
	}

	private void updateClockValue(long value) {
		this.clock.updateAndGet(current -> value);
	}
}
