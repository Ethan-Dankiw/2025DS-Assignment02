package net.ethandankiw.server;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.aggregation.AggregationServer;
import net.ethandankiw.data.server.ServerPool;

public class ServerBalancerImpl implements ServerBalancer {

	// Get the logger for this class
	private static final Logger logger = LoggerFactory.getLogger(ServerBalancerImpl.class);

	// Store the server pool
	private final ServerPool serverPool;

	// Count the number of time the balancer has detected high server load
	private static final Integer HIGH_LOAD_COUNT_THRESHOLD = 5;
	// Count the number of time the balancer has detected low server load
	private static final Integer LOW_LOAD_COUNT_THRESHOLD = 3;
	private Integer highLoadCount = 0;
	private Integer lowLoadCount = 0;


	public ServerBalancerImpl(ServerPool serverPool) {
		this.serverPool = serverPool;
	}


	public void balanceServers() {
		try {
			// Print the stats for each server
			serverPool.printStats();

			// Compute the average server load for every active server
			double averageLoad = serverPool.calculateAverageServerLoad();

			// Log the average load for all servers
			String loadPercentage = String.format("%.2f", averageLoad * 100);
			logger.info("Average server load across {} servers: {}%", serverPool.getServerCount(), loadPercentage);

			// Handle the server balancing
			handleServerBalance(averageLoad);
		} catch (Exception e) {
			logger.error("Error while balancing server count", e);
		}
	}


	private void handleServerBalance(Double serverLoad) {
		// Define strings for the thresholds
		String creationThreshold = String.format("%.2f", GlobalConstants.SERVER_CREATION_THRESHOLD * 100);
		String removalThreshold = String.format("%.2f", GlobalConstants.SERVER_REMOVAL_THRESHOLD * 100);

		// If the server load is getting high
		if (serverLoad > GlobalConstants.SERVER_CREATION_THRESHOLD) {
			logger.info("Attempting to create a new server as load is above threshold: {}%", creationThreshold);
			handleHighLoad();

			// Count the high load detected
			highLoadCount += 1;
			lowLoadCount = 0;

			// Check if the high count has reached its threshold
			if (highLoadCount >= HIGH_LOAD_COUNT_THRESHOLD) {
				// Calculate the load factor
				int loadFactor = highLoadCount - HIGH_LOAD_COUNT_THRESHOLD + 1;

				// Calculate the new scheduling delay
				int period = BalancingScheduler.getDelayPeriod();
				int updatedPeriod = period * (int) Math.pow(loadFactor, 0.9);

				// Ensure the new delay does not fall below a threshold
				updatedPeriod = Math.clamp(updatedPeriod, 5, 180);

				// If the updated period is the same as the old one
				logger.debug("{} high loads were detected in a row", highLoadCount);
				if (period != updatedPeriod) {
					logger.debug("Attempting to change scheduling period from {}s to {}s", period, updatedPeriod);
				}

				// Update the delay and restart the scheduler
				BalancingScheduler.setDelayPeriod(updatedPeriod);
			}
		}

		// If the server load is dropping too low
		else if (serverLoad < GlobalConstants.SERVER_REMOVAL_THRESHOLD) {
			logger.info("Attempting to remove a server as load is below threshold: {}%", removalThreshold);
			handleLowLoad();

			// Count the low load detected
			lowLoadCount += 1;
			highLoadCount = 0;

			// Check if the low count has reached its threshold
			if (lowLoadCount >= LOW_LOAD_COUNT_THRESHOLD) {
				// Calculate the load factor
				int loadFactor = lowLoadCount - LOW_LOAD_COUNT_THRESHOLD + 1;

				// Calculate the new scheduling delay
				int period = BalancingScheduler.getDelayPeriod();
				int updatedPeriod = period * (int) Math.pow(loadFactor, 1.1);

				// Ensure the new delay does not fall below a threshold
				updatedPeriod = Math.clamp(updatedPeriod, 5, 180);

				// If the updated period is the same as the old one
				logger.debug("{} low loads were detected in a row", lowLoadCount);
				if (period != updatedPeriod) {
					logger.debug("Attempting to change scheduling period from {}s to {}s", period, updatedPeriod);
				}

				// Update the delay and restart the scheduler
				BalancingScheduler.setDelayPeriod(updatedPeriod);
			}
		}

		// If the server load is within the defined range
		else {
			logger.info("Server load is within thresholds ({} - {}), no action taken", removalThreshold, creationThreshold);
		}
	}


	private void handleHighLoad() {
		// If the current server count is already at maximum
		if (Objects.equals(serverPool.getServerCount(), GlobalConstants.MAX_SERVERS)) {
			logger.info("Not creating a server, already at maximum count despite a HIGH average server load");
			return;
		}

		// Add a new server to the queue
		serverPool.createAndRegister();
	}


	private void handleLowLoad() {
		// If the current server count is already at minimum
		if (Objects.equals(serverPool.getServerCount(), GlobalConstants.MIN_SERVERS)) {
			logger.info("Not removing a server, already at minimum count despite a LOW average server load");
			return;
		}

		// Remove the least loaded server
		AggregationServer leastLoaded = serverPool.popLeastLoadedServer();

		// If the least loaded server does not exist
		if (leastLoaded == null) {
			logger.error("Unable to stop a server that does not exist");
			return;
		}

		// If the server exists
		logger.info("Marking server for shutdown...");
		gracefullyShutdownServer(leastLoaded);
	}


	private void gracefullyShutdownServer(AggregationServer server) {
		// If the server does not exist
		if (server == null) {
			logger.error("Cannot shut down a server that does not exist");
			return;
		}

		// Do not wait twice if the server is already waiting to finish processing existing requests
		if (server.isDraining()) {
			logger.error("Server is already being shutdown");
			return;
		}

		// Create a new thread that waits for the server to finish processing requests
		new Thread(() -> {
			try {
				// Stop accepting new requests
				server.startDraining();
				// Wait until the server is done processing requests
				server.awaitFinishedProcessing();
				// When the server has no active requests, shut it down
				server.shutdown();
				logger.info("Server {} has shut down gracefully. Remaining = {}", server.getUUID(), serverPool.getServerCount());
			} catch (InterruptedException e) {
				Thread.currentThread()
					  .interrupt();
				logger.warn("Graceful shutdown interrupted", e);
			}
		}, "GracefulServerShutdownThread").start();
	}
}
