package net.ethandankiw.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketUtils {

	public static final Logger logger = LoggerFactory.getLogger(SocketUtils.class);

	// Create a Server Socket on a port
	public static ServerSocket createServerSocket(Integer port) {
		// Attempt to create a server socket
		try (ServerSocket server = new ServerSocket(port)) {
			logger.info("Server started on port {}", port);
			logger.info("Waiting for a client to connect...");

			// Return the socket
			return server;
		} catch (IOException ioe) {
			// Log a severe error since the server cannot be created
			logger.error("[SERVER]: {}", ioe.getMessage());
		}

		// Default to no server
		return null;
	}
}
