package net.ethandankiw.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.HttpServer;
import net.ethandankiw.data.ServerLoadComparator;
import net.ethandankiw.utils.SocketUtils;

public class LoadBalancer {

	// Get the logger for this class
	private static final Logger logger = LoggerFactory.getLogger(LoadBalancer.class);

	// Create a priority queue where the lowest server load is first
	private static final PriorityBlockingQueue<AggregationServer> servers = new PriorityBlockingQueue<>(GlobalConstants.DEFAULT_BALANCED_SERVERS, new ServerLoadComparator());

	// Separate thread for managing server scaling
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	// Create a thread pool for handling client connections
	private static final ExecutorService clientRequestPool = Executors.newFixedThreadPool(100);

	// Store the server that is having its requests balanced
	private static HttpServer listener;


	public static void main(String[] args) {

		// Port to listen for requests on
		// Default: 4567
		Integer serverPort = GlobalConstants.SERVER_PORT;

		// If a different port number is provided
		if (args.length == 1) {
			// Get the argument from the command line
			String arg = args[0];

			// If the argument is valid
			if (arg != null && !arg.isBlank()) {
				// Parse the argument from string to int
				serverPort = Integer.parseInt(args[0]);
			}
		}

		// Create a new HTTP server
		listener = new HttpServer(LoadBalancer.class.getSimpleName(), serverPort);

		// Initialise the server
		listener.start();

		// Create the default amount of aggregation servers
		for (int i = 0; i < GlobalConstants.DEFAULT_BALANCED_SERVERS; i++) {
			// Add a new server to the queue
			addNewServer();
		}

		// Delay first schedule hit
		int initialDelay = 5; // seconds

		// Run scheduler every X seconds
		int delayPeriod = 30; // seconds

		// Start the server scaling thread
		// Every 30s balance the number of active aggregation servers
		scheduler.scheduleAtFixedRate(LoadBalancer::balanceServers, initialDelay, delayPeriod, TimeUnit.SECONDS);
		logger.info("Load Balancer Scheduler started with an initial delay of {} seconds and will run every {} seconds", initialDelay, delayPeriod);

		// Start balancing the incoming requests
		startAcceptingRequests();
	}


	static void startAcceptingRequests() {
		// Get the socket from the HTTP server
		ServerSocket serverSocket = listener.getSocket();

		// If the socket doesn't exist
		if (serverSocket == null) {
			logger.error("Unable to make connection to client as server socket doesn't exist");
			return;
		}

		// Loop infinitely
		while (true) {
			// Accept a connection from a client
			Optional<Socket> optionalConnection = SocketUtils.acceptClientConnection(serverSocket);

			// If the connection was unable to be established
			if (optionalConnection.isEmpty()) {
				logger.error("Unable to make a connection to the client");
				continue;
			}

			// Get the connection
			Socket client = optionalConnection.get();

			// Handle the client request on a separate thread
			clientRequestPool.submit(() -> handleClient(client));
		}
	}


	static void handleClient(Socket client) {
		// Get the first server that is accepting requests with the least load
		AggregationServer leastLoaded = getAvailableServer();

		// If the server exists, handle the client's request
		leastLoaded.handleClientConnection(client);
	}


	static AggregationServer getAvailableServer() {
		// Get the least loaded server
		AggregationServer server = getLeastLoadedServer();

		// If the server does not exist
		if (server == null) {
			logger.debug("Creating new server as no servers currently exist");

			// Create and add a new server to the list
			return addNewServer();
		}

		// If the server does exist
		// Check if the server is at capacity
		if (server.atCapacity()) {
			logger.debug("Creating new server as least loaded server is at capacity");

			// Create and add a new server to the list
			return addNewServer();
		}

		// Return the least loaded server that is not at capacity
		return server;
	}


	static void printServerStats() {
		logger.info("Checking server load...");

		// Define a counter
		// TODO: Create a UUID for each server
		int counter = 1;

		// Log the breakdown of server load for each server
		for (AggregationServer server : servers) {
			logger.info("Server {} has {} active requests out of {}", counter, server.getActiveRequestsCount(), GlobalConstants.MAX_THREADS_FOR_CLIENT_REQUESTS);
			counter++;
		}
	}


	static void balanceServers() {
		try {
			// Print the stats for each server
			printServerStats();

			// Compute the average server load for every active server
			double averageLoad = calculateAverageLoad();

			// Log the average load for all servers
			String loadPercentage = String.format("%.2f", averageLoad * 100);
			logger.info("Average server load across {} servers: {}%", servers.size(), loadPercentage);

			// Handle the server balancing
			handleServerBalance(averageLoad);
		} catch (Exception e) {
			logger.error("Error while balancing server count", e);
		}
	}


	static void handleServerBalance(Double serverLoad) {
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


	private static void handleHighLoad() {
		// If the current server count is already at maximum
		if (servers.size() == GlobalConstants.MAX_SERVERS) {
			logger.info("Unable to create new servers, already at maximum despite a HIGH average server load");
			return;
		}

		// Add a new server to the queue
		addNewServer();
	}


	private static void handleLowLoad() {
		// If the current server count is already at minimum
		if (servers.size() == GlobalConstants.MIN_SERVERS) {
			logger.info("Unable to remove a server, already at minimum despite a LOW average server load");
			return;
		}

		// Remove the least loaded server
		AggregationServer leastLoaded = removeLeastLoadedServer();

		// If the least loaded server does not exist
		if (leastLoaded == null) {
			logger.error("Unable to stop a server that does not exist");
			return;
		}

		// If the server exists
		logger.info("Marking server for shutdown...");
		gracefullyShutdownServer(leastLoaded);
	}


	private static void gracefullyShutdownServer(AggregationServer server) {
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
				logger.info("Server shut down gracefully. Remaining = {}", servers.size());
			} catch (InterruptedException e) {
				Thread.currentThread()
					  .interrupt();
				logger.warn("Graceful shutdown interrupted", e);
			}
		}, "GracefulServerShutdownThread").start();
	}


	private static double calculateAverageLoad() {
		// Calculate the average
		return servers.stream()
					  .mapToDouble(AggregationServer::getLoad)
					  .average()
					  .orElse(0.0);
	}


	static AggregationServer addNewServer() {
		// Create a new server
		AggregationServer server = new AggregationServer();

		// Add the server to the queue
		addServer(server);
		logger.info("Added new server, current active count = {}", servers.size());

		// Return the newly created server
		return server;
	}


	static AggregationServer getLeastLoadedServer() {
		// Get the server with the least load
		AggregationServer server = removeLeastLoadedServer();

		// Put the server back into the list of servers
		addServer(server);

		// Return the least loaded server
		return server;
	}

	static AggregationServer removeLeastLoadedServer() {
		// Get the server with the least load
		return servers.poll();
	}


	static void addServer(AggregationServer server) {
		// If the server does not exist
		if (server == null) {
			logger.info("Cannot add server to priority queue as the server is null");
			return;
		}

		// Put the server back in the list of servers
		boolean success = servers.offer(server);

		// If the server was not put in the list
		if (!success) {
			logger.error("Failed to re-add server to the priority queue: {}", server);
		}
	}
}
