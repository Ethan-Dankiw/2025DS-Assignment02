package net.ethandankiw.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {

	public static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);


	private HttpUtils() {
	}


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


	public static Map<String, String> parseHeaders(BufferedReader fromClient) {
		// Define the map of header key values paris
		Map<String, String> headers = new HashMap<>();

		try {
			// Define a variable for storing the header line
			String headerLine;

			// Loop over the header fields until an empty line or EOF is reached
			while ((headerLine = fromClient.readLine()) != null && !headerLine.isBlank()) {
				// Otherwise parse header fields
				AbstractMap.SimpleEntry<String, String> header = parseSingleHeader(headerLine);
				headers.put(header.getKey(), header.getValue());
			}
		} catch (Exception e) {
			logger.error("Error reading headers: {}", e.getMessage());
		}

		// Return the parsed headers
		return headers;
	}


	// https://www.baeldung.com/java-pairs
	private static AbstractMap.SimpleEntry<String, String> parseSingleHeader(String headerLine) {
		// Split on first colon
		String[] headerKeyValue = headerLine.split(":", 2);

		// Remove whitespace for each value
		String key = headerKeyValue[0].trim();
		String value = headerKeyValue[1].trim();

		// Return a key value pair for the header
		return new AbstractMap.SimpleEntry<>(key, value);
	}
}
