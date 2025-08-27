package net.ethandankiw.server;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.aggregation.AggregationServer;

public class ServerBalancerImpl implements ServerBalancer {

	// Get the logger for this class
	private static final Logger logger = LoggerFactory.getLogger(ServerBalancerImpl.class);

	// Store the server pool
	private final ServerPool serverPool;


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
			logger.info("Creating a new server as load is above threshold: {}", creationThreshold);
			handleHighLoad();
		}

		// If the server load is dropping too low
		else if (serverLoad < GlobalConstants.SERVER_REMOVAL_THRESHOLD) {
			logger.info("Removing a server as load is below threshold: {}", removalThreshold);
			handleLowLoad();
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
