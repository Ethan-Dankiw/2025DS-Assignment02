package net.ethandankiw.aggregation;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.server.BalancingScheduler;
import net.ethandankiw.server.HttpServer;
import net.ethandankiw.server.ServerBalancerImpl;
import net.ethandankiw.server.ServerPoolImpl;
import net.ethandankiw.utils.SocketUtils;

public class LoadBalancer {

	// Get the logger for this class
	private static final Logger logger = LoggerFactory.getLogger(LoadBalancer.class);

	// Create a thread pool for handling client connections
	private static final ExecutorService clientRequestPool = Executors.newFixedThreadPool(100);

	private static ServerPoolImpl serverPool = new ServerPoolImpl(GlobalConstants.DEFAULT_BALANCED_SERVERS);

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

		// Create a new server pool
		serverPool = new ServerPoolImpl(GlobalConstants.DEFAULT_BALANCED_SERVERS);

		// Create the default amount of aggregation servers
		for (int i = 0; i < GlobalConstants.DEFAULT_BALANCED_SERVERS; i++) {
			// Add a new server to the queue
			serverPool.createAndRegister();
		}

		// Create a new server scaler
		ServerBalancerImpl serverBalancer = new ServerBalancerImpl(serverPool);

		// Set the server balancer on the scheduler
		BalancingScheduler.setBalancer(serverBalancer);

		// Start the balancing scheduler
		BalancingScheduler.startBalancingScheduler();

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
		AggregationServer leastLoaded = serverPool.getAvailableServer();

		// If the server exists, handle the client's request
		leastLoaded.handleClientConnection(client);
	}
}
