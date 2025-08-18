package net.ethandankiw;

import java.io.IOException;
import java.net.ServerSocket;

import java.util.logging.Logger;

public class Main {

	private static final Logger logger = Logger.getLogger(Main.class.getName());

	private static final int PORT = 8080;

	public static void main(String[] args) {
		// Create a Server Socket on port 8080
		try (ServerSocket server = new ServerSocket(PORT)) {
			// Accept a connection from the server
		} catch (IOException ioe) {
			// Log a severe error since the server cannot be created
			logger.severe(ioe.getMessage());
		}
	}
}