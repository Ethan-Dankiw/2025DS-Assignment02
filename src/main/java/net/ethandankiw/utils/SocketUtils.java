package net.ethandankiw.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
		try {
			// Attempt to create a server socket
			ServerSocket server = new ServerSocket(port);

			// Return the socket
			return Optional.of(server);
		} catch (IOException ioe) {
			// Log a severe error since the server cannot be created
			logger.error("Unable to create server socket: {}", ioe.getMessage());
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
			logger.warn("Unable to make a connection to the client: {}", ioe.getMessage());
		}

		// Default to no client connection
		return Optional.empty();
	}

	/**
	 * Opens a socket connection from a client to a server.
	 *
	 * @param serverAddress The IP address or hostname of the server.
	 * @param serverPort The port number of the server.
	 * @return An Optional containing the client socket if the connection is successful,
	 * otherwise an empty Optional.
	 */
	public static Optional<Socket> createClientSocket(String serverAddress, int serverPort) {
		try {
			Socket clientSocket = new Socket(serverAddress, serverPort);
			logger.info("Successfully connected to server at {}:{}", serverAddress, serverPort);
			return Optional.of(clientSocket);
		} catch (IOException e) {
			logger.error("Failed to connect to server at {}:{}. Error: {}", serverAddress, serverPort, e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Writes a string message to a socket.
	 *
	 * @param socket The socket to write to.
	 * @param message The string message to send.
	 * @return true if write was successful, false otherwise.
	 */
	public static boolean writeToSocket(Socket socket, String message) {
		try {
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			writer.println(message);
			writer.flush();
			logger.info("Message sent to {}: \n\n{}", socket.getInetAddress(), message);
			return true;
		} catch (IOException e) {
			logger.error("Failed to write to socket {}. Error: {}", socket.getInetAddress(), e.getMessage());
			return false;
		}
	}

	/**
	 * Reads all available text from a socket until the end of the stream is reached.
	 *
	 * @param socket The socket to read from.
	 * @return An Optional containing the entire read string if successful, otherwise an empty Optional.
	 */
	public static Optional<String> readFromSocket(Socket socket) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			StringBuilder fullMessage = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				fullMessage.append(line).append("\n");
			}

			if (!fullMessage.isEmpty()) {
				logger.info("Message received from {}:\n{}", socket.getInetAddress(), fullMessage);
				return Optional.of(fullMessage.toString());
			}

		} catch (IOException e) {
			logger.error("Failed to read from socket {}. Error: {}", socket.getInetAddress(), e.getMessage());
		}

		return Optional.empty();
	}
}
