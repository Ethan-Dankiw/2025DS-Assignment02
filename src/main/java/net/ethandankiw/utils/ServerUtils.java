package net.ethandankiw.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.network.AggregationServer;

public class ServerUtils {

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
