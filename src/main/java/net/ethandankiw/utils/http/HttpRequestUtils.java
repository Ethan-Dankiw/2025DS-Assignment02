package net.ethandankiw.utils.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.data.http.HttpRequest;

public class HttpRequestUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpRequestUtils.class);


	private HttpRequestUtils() {
	}


	/**
	 * Parse a client request
	 */
	public static Optional<HttpRequest> parseClientRequest(Socket client) {
		// Print the lines from the request then close the client socket
		try {
			InputStream stream = client.getInputStream();
			// Get the communication stream being sent from the client
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(stream));

			// Parse the request from the client
			return parseRequest(fromClient);
		} catch (IOException ioe) {
			logger.warn("Unable to get input stream for client: {}", ioe.getMessage());
		}

		// Default to no parsed HTTP request
		return Optional.empty();
	}


	private static Optional<HttpRequest> parseRequest(BufferedReader fromClient) {
		// Create the data structure for a http request
		HttpRequest request = new HttpRequest();

		// Get the request from the client
		List<String> requestLine = HttpRequestUtils.parseRequestLine(fromClient);

		// If there is a valid request line
		if (requestLine.isEmpty()) {
			logger.error("Invalid request as there is no request line");
			return Optional.empty();
		}

		// Store the request line on the request object
		request.parseAndSetMethod(requestLine.getFirst());
		request.setPath(requestLine.get(1));
		request.setVersion(requestLine.get(2));

		// Get the header lines from the client
		Map<String, String> headers = HttpRequestUtils.parseHeaders(fromClient);

		// If there are no headers
		if (headers.isEmpty()) {
			logger.error("Invalid request as there are no headers");
			return Optional.empty();
		}

		// Store the headers on the request object
		request.setHeaders(headers);

		try {
			// Get the content length of the body
			String contentLengthStr = headers.get("content-length");

			// If there is no content length header
			if (contentLengthStr == null) {
				logger.error("Content Length header is missing");
				return Optional.empty();
			}

			// Parse the content length string into a value
			int contentLength = Integer.parseInt(contentLengthStr);

			// Parse the body from the client using the content length
			String body = HttpRequestUtils.parseBody(fromClient, contentLength);

			// Store the body on the request
			request.setBody(body);
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
				headers.put(header.getKey()
								  .toLowerCase(), header.getValue());
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
