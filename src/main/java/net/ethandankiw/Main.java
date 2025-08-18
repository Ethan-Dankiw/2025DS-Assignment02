package net.ethandankiw;

import java.io.IOException;
import java.net.ServerSocket;

import java.net.Socket;
import java.util.logging.Logger;

public class Main {

	private static final Logger logger = Logger.getLogger(Main.class.getName());

	private static final int PORT = 8080;

	public static void main(String[] args) {
		// Create a Server Socket on port 8080
		try (ServerSocket server = new ServerSocket(PORT)) {
			logger.info("Server started on port " + PORT);
			logger.info("Waiting for a client to connect...");

			// Accept a connection from the server
			try (Socket client = server.accept()) {

			} catch (IOException ioe) {
				// Log a warning since the client connection isn't as important
				logger.warning("[CLIENT]: " + ioe.getMessage());
			}
		} catch (IOException ioe) {
			// Log a severe error since the server cannot be created
			logger.severe("[SERVER]: " + ioe.getMessage());
		}
	}
}