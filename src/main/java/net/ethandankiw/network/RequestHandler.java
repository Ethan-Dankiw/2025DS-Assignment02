package net.ethandankiw.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.HttpRequest;
import net.ethandankiw.utils.HttpParser;
import net.ethandankiw.utils.SocketUtils;

public class RequestHandler {

	private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
	// Define a pool of threads to handle client requests
	ExecutorService pool = Executors.newFixedThreadPool(GlobalConstants.MAX_THREADS_FOR_CLIENT_REQUESTS);

	/**
	 * Accept a new connection to the server
	 */
	public void acceptConnection(ServerSocket server) {
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
		pool.submit(() -> parseClientRequest(client));
	}


	/**
	 * Handle a client request
	 */
	private void parseClientRequest(Socket client) {
		// Print that a new connection is being handled
		logger.info("Handling new client connection");

		// Print the lines from the request then close the client socket
		try (InputStream stream = client.getInputStream()) {
			// Get the communication stream being sent from the client
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(stream));

			// Parse the request from the client
			Optional<HttpRequest> optionalRequest = HttpParser.parseRequest(fromClient);

			// If the request is invalid
			if (optionalRequest.isEmpty()) {
				logger.warn("Invalid request from client");
				return;
			}

			// If the request is valid
			HttpRequest request = optionalRequest.get();

			// Print the request
			logger.debug("Parsed HTTP Request: {}", request);

			// Handle the request
			RequestHandler.handleRequest(request);

		} catch (IOException ioe) {
			logger.warn("Unable to get input stream for client: {}", ioe.getMessage());
		} finally {
			// Always close client request after being handled
			try {
				client.close();
				logger.info("Connection closed");
			} catch (IOException ioe) {
				logger.error("Unable to close client connection: {}", ioe.getMessage());
			}
		}
	}
}
