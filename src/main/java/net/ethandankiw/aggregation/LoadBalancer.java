package net.ethandankiw.aggregation;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.HttpRequest;
import net.ethandankiw.data.HttpServer;
import net.ethandankiw.utils.RequestUtils;
import net.ethandankiw.utils.SocketUtils;

public class LoadBalancer {

	private static final Logger logger = LoggerFactory.getLogger(LoadBalancer.class);

	// Define a pool of threads to handle client requests
	ExecutorService pool = Executors.newFixedThreadPool(GlobalConstants.MAX_THREADS_FOR_CLIENT_REQUESTS);

	// Store the server that is having its requests balanced
	HttpServer server;


	public LoadBalancer(@NotNull HttpServer server) {
		this.server = server;
	}


	public void startListening() {
		// Continuously accept connections from clients
		while (true) {
			logger.info("Waiting for a client to connect...");

			// Accept a connection from a client to the server
			this.acceptConnection();
		}
	}


	private void acceptConnection() {
		// Get the socket from the HTTP server
		ServerSocket socket = server.getSocket();

		// If the socket doesn't exist
		if (socket == null) {
			logger.error("Unable to make connection to client as server socket doesn't exist");
			return;
		}

		// Accept a connection from a client
		Optional<Socket> optionalConnection = SocketUtils.acceptClientConnection(server.getSocket());

		// If the connection was unable to be established
		if (optionalConnection.isEmpty()) {
			logger.error("Unable to make a connection to the client");
			return;
		}

		// Get the connection
		Socket client = optionalConnection.get();

		// Spawn a new thread from the pool to process the client request
		logger.debug("Spawning new thread to handle connection");
		pool.submit(() -> handleConnection(client));
	}

	private void handleConnection(Socket client) {
		// Parse a possible request from the client
		Optional<HttpRequest> optionalRequest = RequestUtils.parseClientRequest(client);

		// Print that the request was parsed from the connection
		logger.debug("Client request has been parsed");

		// If the request was unable to be parsed
		if (optionalRequest.isEmpty()) {
			logger.error("Unable to parse client request");
			return;
		}

		// If request is valid
		HttpRequest request = optionalRequest.get();

		// Print that a new connection is being handled
		logger.debug("Client request is being handled");

		// Handle the request from the client
		// TODO: Send a response back to client
		RequestUtils.handleClientRequest(request);

		try {
			client.close();
			logger.info("Connection closed");
		} catch (IOException ioe) {
			logger.error("Unable to close client connection: {}", ioe.getMessage());
		}
	}
}
