package net.ethandankiw.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.data.HttpRequest;

public class RequestUtils {

	private static final Logger logger = LoggerFactory.getLogger(RequestUtils.class);


	private RequestUtils() {
	}


	/**
	 * Parse a client request
	 */
	public static Optional<HttpRequest> parseClientRequest(Socket client) {
		// Print the lines from the request then close the client socket
		try (InputStream stream = client.getInputStream()) {
			// Get the communication stream being sent from the client
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(stream));

			// Parse the request from the client
			return HttpUtils.parseRequest(fromClient);
		} catch (IOException ioe) {
			logger.warn("Unable to get input stream for client: {}", ioe.getMessage());
		}

		// Default to no parsed HTTP request
		return Optional.empty();
	}


	/**
	 * Handle a client request
	 */
	public static void handleClientRequest(HttpRequest req) {
		// TODO: Something
	}
}
