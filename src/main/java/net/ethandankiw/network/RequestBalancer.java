package net.ethandankiw.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.utils.HttpParser;
import net.ethandankiw.utils.SocketUtils;

public class RequestBalancer {

	private static final Logger logger = LoggerFactory.getLogger(RequestBalancer.class);
	// Define a pool of threads to handle client requests
	ExecutorService pool = Executors.newFixedThreadPool(GlobalConstants.MAX_THREADS_FOR_CLIENT_REQUESTS);
	// Store the server socket for load balancing
	private ServerSocket server = null;


	/**
	 * Constructor for initialising the origin server to balance client requests to
	 */
	public RequestBalancer(ServerSocket server) {
		this.server = server;
	}


	/**
	 * Start accepting connections for the server
	 */
	public void startAcceptingConnections() {
		// Always accept connections from clients to the server
		while (true) {
			acceptConnection();
		}
	}


	/**
	 * Accept a new connection to the server
	 */
	private void acceptConnection() {
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
	private void handleClientRequest(Socket client) {
		// Print that a new connection is being handled
		logger.info("Handling new client connection");

		// Print the lines from the request then close the client socket
		try (InputStream stream = client.getInputStream()) {
			// Get the communication stream being sent from the client
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(stream));

			// Get the request line from the client
			Optional<String> optionalRequestLine = HttpParser.parseRequestLine(fromClient);

			// If the request line is invalid
			if (optionalRequestLine.isEmpty()) {
				logger.error("Unable to process client request as it is invalid");
				return;
			}

			logger.info("Parsed Request Line: {}", optionalRequestLine.get());

			// Get the header lines from the client
			Map<String, String> headers = HttpParser.parseHeaders(fromClient);

			// If there are no headers
			if (headers.isEmpty()) {
				logger.error("Invalid request as there are no headers");
				return;
			}

			headers.forEach((key, value) -> logger.info("Parsed Header: {} -> {}", key, value));

			try {
				// Get the content length of the body
				String contentLengthStr = headers.get("Content-Length");
				// Parse the content length string into a value
				int contentLength = Integer.parseInt(contentLengthStr);

				// Parse the body from the client using the content length
				String body = HttpParser.parseBody(fromClient, contentLength);

				logger.info("Parsed Body: {}", body);
			} catch (NumberFormatException nfe) {
				logger.error("Unable to parse body as content length is invalid: {}", nfe.getMessage());
			}
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
