package net.ethandankiw.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.HttpServer;
import net.ethandankiw.utils.SocketUtils;

public class LoadBalancer {

	// Get the logger for this class
	private static final Logger logger = LoggerFactory.getLogger(LoadBalancer.class);

	// Create a priority queue where the lowest server load is first
	private static final PriorityQueue<AggregationServer> servers = new PriorityQueue<>(new ServerLoadComparator());

	// Create a lock on the queue of servers
	private static final ReentrantLock lock = new ReentrantLock();

	// Store the server that is having its requests balanced
	private static HttpServer listener;

	// Separate thread for managing server scaling
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


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
			// Create an aggregation server
			AggregationServer server = new AggregationServer();

			// Put the server on the queue
			servers.add(server);
		}

		// Delay first schedule hit
		int initialDelay = 30; // seconds

		// Run scheduler every X seconds
		int delayPeriod = 30; // seconds

		// Start the server scaling thread
		// Every 30s balance the number of active aggregation servers
		scheduler.scheduleAtFixedRate(LoadBalancer::balanceServerCount, initialDelay, delayPeriod, TimeUnit.SECONDS);
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

			// Get the lock for the servers list
			lock.lock();

			// Get the server that has the least load
			AggregationServer leastLoadedServer = servers.poll();

			// If the server does not exist
			if (leastLoadedServer == null) {
				throw new MissingResourceException(
						"No server to process the incoming request",
						LoadBalancer.class.getSimpleName(),
						AggregationServer.class.getSimpleName()
				);
			}

			// If the server exists
			// Add the server back and sort based on current load
			servers.add(leastLoadedServer);

			// Release the lock
			lock.unlock();



			// If the server exists
			leastLoadedServer.handleClientConnection(client);
		}
	}


	static void balanceServerCount() {
		try {
			logger.info("Checking server load...");

			// Compute the average server load for every active server
			double averageLoad = calculateAverageLoad();

			logger.info("Average server load across {} servers: {}%", servers.size(), averageLoad);

			// If the average server load is getting high
			if (averageLoad > 0.75) {
				handleHighLoad();
			}

			// If the average server load is dropping too low
			else if (averageLoad < 0.25) {
				handleLowLoad();
			}

		} catch (Exception e) {
			logger.error("Error while balancing server count", e);
		}
	}


	private static double calculateAverageLoad() {
		// Calculate the average
		return servers.stream()
					  .mapToDouble(AggregationServer::getLoad)
					  .average()
					  .orElse(0.0);
	}


	private static void handleHighLoad() {
		// If the current server count is already at maximum
		if (servers.size() == GlobalConstants.MAX_SERVERS) {
			logger.info("Unable to create new servers, already at maximum despite a HIGH average server load");
			return;
		}

		AggregationServer newServer = new AggregationServer();
		servers.add(newServer);
		logger.info("Added new server, current active count = {}", servers.size());
	}


	private static void handleLowLoad() {
		// If the current server count is already at minimum
		if (servers.size() == GlobalConstants.MIN_SERVERS) {
			logger.info("Unable to remove a server, already at minimum despite a LOW average server load");
			return;
		}

		// Get the least loaded server
		AggregationServer leastLoaded = servers.poll();

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

}
