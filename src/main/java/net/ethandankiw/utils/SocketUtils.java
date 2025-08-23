package net.ethandankiw.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketUtils {

	public static final Logger logger = LoggerFactory.getLogger(SocketUtils.class);


	private SocketUtils() {
	}


	// Create a Server Socket on a port
	public static Optional<ServerSocket> createServerSocket(Integer port) {
		// Attempt to create a server socket
		try {
			ServerSocket server = new ServerSocket(port);
			logger.info("Server started on port {}", port);
			logger.info("Waiting for a client to connect...");

			// Return the socket
			return Optional.of(server);
		} catch (IOException ioe) {
			// Log a severe error since the server cannot be created
			logger.error("[SERVER]: {}", ioe.getMessage());
		}

		// Default to no server
		return Optional.empty();
	}


	// Accept a client connection to a server
	public static Optional<Socket> acceptClientConnection(ServerSocket server) {
		// Accept a connection from the server
		try {
			Socket client = server.accept();
			logger.info("Client connected: {}", client.getInetAddress());

			return Optional.of(client);
		} catch (IOException ioe) {
			// Log a warning since the client connection isn't as important
			logger.warn("[CLIENT]: {}", ioe.getMessage());
		}

		// Default to no client connection
		return Optional.empty();
	}
}
