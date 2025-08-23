package net.ethandankiw.network;

import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.ConnectException;
import java.util.MissingResourceException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.utils.SocketUtils;

public class LoadBalancer {

	private static final Logger logger = LoggerFactory.getLogger(LoadBalancer.class);

	// Store the server socket for load balancing
	private static ServerSocket _server = null;

	// Define a pool of threads to handle client requests
	ExecutorService pool = Executors.newFixedThreadPool(100); // cap at 100

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
		_server = optionalServer.get();
	}

	/**
	 * Accept a new connection to the server
	 */
	public void acceptConnection() {
		// If the server isn't initialised
		if (_server == null) {
			throw new NullPointerException("Server Socket does not exist");
		}

		// Accept a connection from a client
		Optional<Socket> optionalConnection = SocketUtils.acceptClientConnection(_server);

		// If the connection was unable to be established
		if (optionalConnection.isEmpty()) {
			logger.error("Unable to accept a connection to the client");
			return;
		}

		// Get the connection
		Socket client = optionalConnection.get();

		// Handle the client connection on a new thread

	}
}
