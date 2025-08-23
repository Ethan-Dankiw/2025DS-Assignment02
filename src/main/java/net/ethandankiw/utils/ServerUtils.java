package net.ethandankiw.utils;

import java.net.ServerSocket;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerUtils {

	private static final Logger logger = LoggerFactory.getLogger(ServerUtils.class);


	private ServerUtils() {
	}


	// Create a http server on a given port
	public static ServerSocket createServer(Integer port) {
		// Start up a socket server on the given port
		Optional<ServerSocket> optionalServer = SocketUtils.createServerSocket(port);

		// If the server doesn't exist
		if (optionalServer.isEmpty()) {
			logger.error("Unable to create server socket");
			return null;
		}

		// If it exists, get the socket server
		return optionalServer.get();
	}
}
