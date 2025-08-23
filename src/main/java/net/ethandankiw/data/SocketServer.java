package net.ethandankiw.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;

import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;

public class SocketServer {

	private static final Logger logger = LoggerFactory.getLogger(SocketServer.class);

	public static void main(String[] args) {
		// Create a Server Socket on port 8080
		try (ServerSocket server = new ServerSocket(GlobalConstants.SERVER_PORT)) {
			logger.info("Server started on port " + GlobalConstants.SERVER_PORT);
			logger.info("Waiting for a client to connect...");

			// Accept a connection from the server
			try (Socket client = server.accept()) {
				logger.info("Client connected: " + client.getInetAddress());

				// Get the communication stream being sent from the client
				BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));

				// Get the communication stream for sending stuff to the client
				PrintWriter toClient = new PrintWriter(client.getOutputStream(), true);

				// Read from the client
				fromClient.lines().forEach(line -> {
					logger.info("Client says: " + line);
					toClient.println("Server received: " + line);
				});
			} catch (IOException ioe) {
				// Log a warning since the client connection isn't as important
				logger.warn("[CLIENT]: " + ioe.getMessage());
			}
		} catch (IOException ioe) {
			// Log a severe error since the server cannot be created
			logger.error("[SERVER]: " + ioe.getMessage());
		}
	}
}