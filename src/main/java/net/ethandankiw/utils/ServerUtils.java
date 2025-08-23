package net.ethandankiw.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerUtils {

	private ServerUtils() {}

	private static final Logger logger = LoggerFactory.getLogger(ServerUtils.class);

	// Create a http server on a given port
	public static void createServer(Integer port) {
		// Start up a socket server on the given port
		Optional<ServerSocket> optionalServer = SocketUtils.createServerSocket(port);

		// If the server doesn't exist
		if (optionalServer.isEmpty()) {
			logger.error("Unable to create server socket");
			return;
		}

		// If it exists, get the socket server
		ServerSocket server = optionalServer.get();

		// Always close the server if any errors arise
		try {
			// Always accept connections from clients
			while (true) {
				// Accept a connection from a client
				Optional<Socket> optionalConnection = SocketUtils.acceptClientConnection(server);

				// If the connection was unable to be established
				if (optionalConnection.isEmpty()) {
					logger.error("Unable to accept a connection to the client");
					continue;
				}

				// Always close the client connection
				try (Socket client = optionalConnection.get()) {
					// Get the communication stream being sent from the client
					BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));

					// Get the communication stream for sending stuff to the client
					PrintWriter toClient = new PrintWriter(client.getOutputStream(), true);

					// Read from the client
					fromClient.lines().forEach(line -> {
						logger.info("Client says: {}", line);
						toClient.println("Server received: " + line);
					});
				} catch (IOException ioe) {
					logger.warn("Client connection error: {}", ioe.getMessage());
				}
			}
		} finally {
			// Attempt to close the server socket
			try {
				server.close();
				logger.info("Server socket closed");
			} catch (IOException ioe) {
				logger.error("Unable to close sever socket: {}", ioe.getMessage());
			}
		}
	}

}
