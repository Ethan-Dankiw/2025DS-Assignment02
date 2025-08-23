package net.ethandankiw.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.utils.SocketUtils;

public class AggregationServer {
	private static final Logger logger = LoggerFactory.getLogger(AggregationServer.class);

	public static void main(String[] args) {

		// Port of the Aggregation server
		// Default: 4567
		Integer AggregationPort = GlobalConstants.SERVER_PORT;

		// If a different port number is provided
		if (args.length == 1) {
			// Get the argument from the command line
			String arg = args[0];

			// If the argument is valid
			if (arg != null && !arg.isBlank()) {
				// Parse the argument from string to int
				AggregationPort = Integer.parseInt(args[0]);
			}
		}

		// Start up a socket server on the given port
		Optional<ServerSocket> optionalServer = SocketUtils.createServerSocket(AggregationPort);

		// If the server doesn't exist
		if (optionalServer.isEmpty()) {
			logger.error("Unable to create server socket");
			return;
		}

		// If it exists, get the socket server
		ServerSocket server = optionalServer.get();

		// Accept a connection from a client
		Optional<Socket> optionalConnection = SocketUtils.acceptClientConnection(server);

		// If the connection doesn't exist
		if (optionalConnection.isEmpty()) {
			logger.error("Unable to accept a connection to the client");
			return;
		}

		// Get the connection
		Socket client = optionalConnection.get();

		try {
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
			// Log a warning since the client connection isn't as important
			logger.warn("[CLIENT]: {}", ioe.getMessage());
		}

		try {
			// Close the server socket
			server.close();
		} catch (IOException ioe) {
			logger.error("Unable to close server connection");
		}
	}
}
