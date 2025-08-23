package net.ethandankiw.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.data.HttpRequest;

public class HttpParser {

	public static final Logger logger = LoggerFactory.getLogger(HttpParser.class);


	private HttpParser() {
	}


	public static Optional<HttpRequest> parseRequest(BufferedReader fromClient) {
		// Create the data structure for a http request
		HttpRequest request = new HttpRequest();

		// Get the request from the client
		List<String> requestLine = HttpParser.parseRequestLine(fromClient);

		// If there is a valid request line
		if (requestLine.isEmpty()) {
			logger.error("Invalid request as there is no request line");
			return Optional.empty();
		}

		// Store the request line on the request object
		request.parseAndSetMethod(requestLine.getFirst());
		request.setPath(requestLine.get(1));

		// Get the header lines from the client
		Map<String, String> headers = HttpParser.parseHeaders(fromClient);

		// If there are no headers
		if (headers.isEmpty()) {
			logger.error("Invalid request as there are no headers");
			return Optional.empty();
		}

		// Store the headers on the request object
		request.setHeaders(headers);

		try {
			// Get the content length of the body
			String contentLengthStr = headers.get("Content-Length");
			// Parse the content length string into a value
			int contentLength = Integer.parseInt(contentLengthStr);

			// Parse the body from the client using the content length
			String body = HttpParser.parseBody(fromClient, contentLength);
		} catch (NumberFormatException nfe) {
			logger.error("Unable to parse body as content length is invalid: {}", nfe.getMessage());
			return Optional.empty();
		}

		return Optional.of(request);
	}


	private static List<String> parseRequestLine(BufferedReader fromClient) {
		try {
			// Read the request line (e.g., "GET /path HTTP/1.1")
			String request = fromClient.readLine();

			// Check if the request line is invalid
			if (request == null || request.isBlank()) {
				return List.of();
			}

			// Split the request line by whitespace (e.g., ["GET", "/path", "HTTP/1.1"])
			String[] splitRequest = request.split(" ");

			// Ensure that the length is valid
			if (splitRequest.length != 3) {
				logger.error("Request line has an invalid number of parameters: {}", request);
				return List.of();
			}

			// If valid, return the request line
			return List.of(splitRequest[0], splitRequest[1], splitRequest[2]);
		} catch (IOException ioe) {
			logger.error("Unable to parse request line: {}", ioe.getMessage());
		}

		// If invalid request, return empty string
		return List.of();
	}


	private static Map<String, String> parseHeaders(BufferedReader fromClient) {
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


	private static String parseBody(BufferedReader fromClient, Integer contentLength) {
		// If no content length is provided
		if (contentLength <= 0) {
			return "";
		}

		// Define the character buffer to read the body to
		char[] buffer = new char[contentLength];

		try {
			// Attempt to read the body to the character buffer
			int charsRead = fromClient.read(buffer, 0, contentLength);

			// If the number of chars read is valid
			if (charsRead > 0) {
				// Parse the buffer to a string
				return new String(buffer, 0, charsRead);
			}
		} catch (IOException ioe) {
			logger.warn("Unable to read body to buffer: {}", ioe.getMessage());
			return "";
		}

		// By default, return an empty string
		return "";
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
