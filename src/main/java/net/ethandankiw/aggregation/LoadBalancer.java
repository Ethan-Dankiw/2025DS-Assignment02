package net.ethandankiw.aggregation;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.LamportClock;
import net.ethandankiw.data.server.ServerPoolImpl;
import net.ethandankiw.data.store.ContentStore;
import net.ethandankiw.server.BalancingScheduler;
import net.ethandankiw.server.HttpServer;
import net.ethandankiw.server.ServerBalancerImpl;
import net.ethandankiw.utils.SocketUtils;

public class LoadBalancer {

	// Logger for this class
	private static final Logger logger = LoggerFactory.getLogger(LoadBalancer.class);

	// Lamport clock for determining the order of received requests
	private static LamportClock clock;

	// Thread pool for handling client connections
	private static ExecutorService clientRequestPool;

	// Server pool of aggregation servers
	private static ServerPoolImpl serverPool;

	// HTTP server that listens for requests
	private static HttpServer clientListener;

	// Accepting new requests
	private static boolean acceptingRequests;


	public static void main(String[] args) {
		// Init the thread pool
		clientRequestPool = Executors.newFixedThreadPool(GlobalConstants.MAX_SERVERS);

		// Start a new lamport clock
		clock = new LamportClock();

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
		clientListener = new HttpServer(LoadBalancer.class.getSimpleName(), serverPort);

		// Initialise the server
		clientListener.start();

		// Create a new server pool
		serverPool = new ServerPoolImpl(GlobalConstants.DEFAULT_BALANCED_SERVERS);

		// Create a new server scaler
		ServerBalancerImpl serverBalancer = new ServerBalancerImpl(serverPool);

		// Set the server balancer on the scheduler
		BalancingScheduler.setBalancer(serverBalancer);

		// Start the balancing scheduler
		BalancingScheduler.startBalancingScheduler();

		// Initialise the content store
		ContentStore.init();

		// Load the content store from disk
		ContentStore.loadFromDisk();

		// Start the Content Store expiry task
		ContentStore.startExpiryTask();

		// Start processing content store requests
		ContentStore.startProcessorThread();

		// Start accepting requests
		acceptingRequests = true;

		// Start balancing the incoming requests
		startAcceptingRequests();
	}


	static void startAcceptingRequests() {
		// Get the socket from the HTTP server
		ServerSocket serverSocket = clientListener.getSocket();

		// If the socket doesn't exist
		if (serverSocket == null) {
			logger.error("Unable to make connection to client as server socket doesn't exist");
			return;
		}

		// Loop infinitely
		while (acceptingRequests) {
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
		AggregationServer leastLoaded = serverPool.getAvailableServer();

		// Handle the client's request on the server
		leastLoaded.handleClientConnection(client, clock);
	}


	public static void shutdown() {
		logger.info("Shutting down LoadBalancer...");
		// Close the server socket
		if (clientListener != null) {
			try {
				clientListener.shutdown();
			} catch (IOException e) {
				logger.error("Failed to close client listener", e);
			}
		}

		// Shut down the thread pool
		clientRequestPool.shutdown();
		try {
			if (!clientRequestPool.awaitTermination(60, TimeUnit.SECONDS)) {
				clientRequestPool.shutdownNow();
			}
		} catch (InterruptedException e) {
			clientRequestPool.shutdownNow();
			Thread.currentThread()
				  .interrupt();
		}

		// Shut down other services
		BalancingScheduler.shutdown();
		ContentStore.stopExpiryTask();
		ContentStore.stopProcessorThread();
		logger.info("LoadBalancer shutdown complete.");
	}


	public static void reset() {
		clientListener = null;
		serverPool = null;

		acceptingRequests = false;

		BalancingScheduler.reset();
		ContentStore.reset();
	}
}
