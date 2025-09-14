package net.ethandankiw.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LamportClock {
	private static final Logger logger = LoggerFactory.getLogger(LamportClock.class);

	private volatile long clock;

	public LamportClock() {
		this.clock = 0;
	}

	public synchronized long getClock() {
		return clock;
	}

	public synchronized void tick() {
		this.clock++;
		logger.debug("Clock ticked to {}", this.clock);
	}

	public synchronized void receive(long receivedClock) {
		// Update the lamport clock according to the received clock value
		this.updateClock(receivedClock);
		logger.debug("Clock received value {}, new clock is {}", receivedClock, this.clock);
	}

	private synchronized void updateClock(long received) {
		this.clock = this.compareLocal(received) + 1;
	}

	private synchronized long compareLocal(long received) {
		return this.compare(this.clock, received);
	}

	private synchronized long compare(long local, long received) {
		return Math.max(local, received);
	}
}
