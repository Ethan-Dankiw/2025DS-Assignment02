package net.ethandankiw.network;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.utils.SocketUtils;

public class LoadBalancer {

	private static final Logger logger = LoggerFactory.getLogger(LoadBalancer.class);

	// Store the server socket for load balancing
	private static ServerSocket server = null;

	// Define a pool of threads to handle client requests
	ExecutorService pool = Executors.newFixedThreadPool(GlobalConstants.MAX_THREADS_FOR_CLIENT_REQUESTS);

	/**
	 * Constructor for initialising the origin server to balance client requests to
	 */
	public LoadBalancer(Integer serverPort) throws MissingResourceException {
		// Start up a socket server on the given port
		Optional<ServerSocket> optionalServer = SocketUtils.createServerSocket(serverPort);

		// If the server doesn't exist
		if (optionalServer.isEmpty()) {
			throw new MissingResourceException("Server Socket cannot be created", LoadBalancer.class.getName(), ServerSocket.class.getName());
		}

		// If it exists, get the socket server
		server = optionalServer.get();
	}

	/**
	 * Accept a new connection to the server
	 */
	public void acceptConnection() {
		// If the server isn't initialised
		if (server == null) {
			throw new NullPointerException("Server Socket does not exist");
		}

		// Accept a connection from a client
		Optional<Socket> optionalConnection = SocketUtils.acceptClientConnection(server);

		// If the connection was unable to be established
		if (optionalConnection.isEmpty()) {
			logger.error("Unable to accept a connection to the client");
			return;
		}

		// Get the connection
		Socket client = optionalConnection.get();

		// Handle the client connection on a new thread
		// Extra clients that would exceed the max thread count wait in a queue
		pool.submit(() -> handleClientRequest(client));
	}

	/**
	 * Handle a client request
	 */
	public void handleClientRequest(Socket client) {

	}
}
