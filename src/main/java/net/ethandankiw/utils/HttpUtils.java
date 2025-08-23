package net.ethandankiw.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {

	public static Logger logger = LoggerFactory.getLogger(HttpUtils.class);

	public static Optional<String> parseRequestLine(BufferedReader fromClient) {
		try {
			// Read the request line (e.g., "GET /path HTTP/1.1")
			String request = fromClient.readLine();

			// Check if the request line is invalid
			if (request == null || request.isBlank()) {
				return Optional.empty();
			}

			// If valid, return the request line
			return Optional.of(request);
		} catch (IOException ioe) {
			logger.error("Unable to parse request line: {}", ioe.getMessage());
		}

		// If invalid request, return empty string
		return Optional.empty();
	}
}
