package net.ethandankiw.utils.http;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.LamportClock;
import net.ethandankiw.data.http.HttpResponse;
import net.ethandankiw.data.http.HttpStatusCode;
import net.ethandankiw.utils.SocketUtils;

public class HttpResponseUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpResponseUtils.class);


	private HttpResponseUtils() {
	}


	public static void generateAndSendResponse(Socket client, HttpStatusCode status, @NotNull String body, LamportClock clock) {
		// Generate a response
		// Increment clock value as response counts as causal event
		clock.tick();
		HttpResponse response = HttpResponseUtils.generateResponse(status, body, clock.getClockValue());

		// Send the complete response to the client
		boolean success = SocketUtils.writeToSocket(client, response.toString());

		// If the response cannot be written to the client
		if (!success) {
			logger.error("Unable to send response to client");
		}
	}


	public static HttpResponse generateResponse(HttpStatusCode status, @NotNull String body, long clockValue) {
		// If there is no status
		if (status == null || status.equals(HttpStatusCode.NONE)) {
			status = HttpStatusCode.BAD_REQUEST;
		}

		// Create a new response object
		HttpResponse response = new HttpResponse();

		// Set the status and the body
		response.setStatus(status);

		// Get the status code and reason phrase
		Integer code = status.getStatusCode();

		// Add the lamport clock to the response
		response.addHeader(GlobalConstants.LAMPORT_CLOCK_HEADER, String.valueOf(clockValue));

		// If the response is successful
		if (code >= 200 && code < 300) {
			logger.debug("Successful Response Detected: {} {}", code, status.getReasonPhrase());
			// A 204 No Content response should not have a body or content-related headers
			if (code == 204) {
				response.setBody("");
			} else {
				response.setBody(body);
				response.addHeader("Content-Type", "application/json");
				response.addHeader("Content-Length", String.valueOf(body.length()));
			}
		}
		// If the response is a client or server error
		else if (code >= 400 && code < 600) {
			logger.debug("Unsuccessful Response Detected: {} {}", code, status.getReasonPhrase());
			response.setBody(body);
			response.addHeader("Content-Type", "text/plain");
			response.addHeader("Content-Length", String.valueOf(body.length()));
		}

		// Return the response object
		return response;
	}


	/**
	 * Parse a client request
	 */
	public static Optional<HttpResponse> parseServerResponse(Socket server) {
		// If the server socket does not exist
		if (server == null) {
			logger.error("Server socket does not exist");
			return Optional.empty();
		}

		// Read the response from the server
		Optional<String> optionalResponse = SocketUtils.readFromSocket(server);

		// If there was no response
		if (optionalResponse.isEmpty()) {
			logger.error("Response from server does not exist");
			return Optional.empty();
		}

		// Get the response as a string
		String responseStr = optionalResponse.get();

		// If the response has no content
		if (responseStr.isEmpty()) {
			logger.error("Response from server was empty");
			return Optional.empty();
		}

		// Parse the response from the server
		return parseResponse(responseStr);
	}


	private static Optional<HttpResponse> parseResponse(String responseStr) {
		// Create a new HTTP Response object
		HttpResponse response = new HttpResponse();

		// Partition the response
		Optional<List<String>> optionalPartition = partitionResponse(responseStr);

		// If the partition was not successful
		if (optionalPartition.isEmpty()) {
			logger.error("Unable to partition response string");
			return Optional.empty();
		}

		// Get the partitions
		List<String> partitions = optionalPartition.get();

		// Extract the partitions
		String statusLine = partitions.get(0);
		String headersLine = partitions.get(1);
		String body = partitions.get(2);

		// Parse the response status line and populate the response object
		boolean statusSuccess = populateResponseStatus(statusLine, response);

		// If the population was not successful
		if (!statusSuccess) {
			logger.error("Unable to parse response status from server");
			return Optional.empty();
		}

		// Parse the response headers and populate the response object
		boolean headerSuccess = populateHeaders(headersLine, response);

		// If the population was not successful
		if (!headerSuccess) {
			logger.error("Unable to parse response headers from server");
			return Optional.empty();
		}

		// If the response has no response body
		// Status Code 204 == No Content
		if (response.getStatusCode() == 204) {
			return Optional.of(response);
		}

		// Verify that the response body exists
		if (body == null || body.isBlank()) {
			logger.error("Unable to parse response body from server");
			return Optional.empty();
		}

		// Populate the body onto the response object
		response.setBody(body.trim());

		// Return the parsed response
		return Optional.of(response);
	}


	private static Optional<List<String>> partitionResponse(String responseStr) {
		// Define the partition in the response [statusLine, headers, body]
		List<String> partitions = new ArrayList<>();

		// Split the response string into response status line + rest of response
		String[] splitResponse = responseStr.split("\n", 2);

		// If split response is not partitioned correctly
		if (splitResponse.length != 2) {
			logger.error("Unable to extract status line from response string");
			return Optional.empty();
		}

		// Get the status line string
		String statusLine = splitResponse[0];
		String headerBody = splitResponse[1];

		// Put the status line on the partition
		partitions.add(statusLine);

		// Split the remaining header + body
		String[] splitHeaderBody = headerBody.split("\n\n", 2);

		// If split header + body is not partitioned correctly
		if (splitHeaderBody.length != 2) {
			logger.error("Unable to extract headers from response string");
			return Optional.empty();
		}

		// Add the headers to the partition
		partitions.add(splitHeaderBody[0]);

		// Add the body to the partition
		partitions.add(splitHeaderBody[1]);

		// Return all the response partitions
		return Optional.of(partitions);
	}


	private static boolean populateResponseStatus(String statusLine, HttpResponse response) {
		// If there is no status line to parse
		if (statusLine == null || statusLine.isBlank()) {
			logger.error("Status line does not exist");
			return false;
		}

		// Split the status line by spaces
		String[] splitStatusLine = statusLine.split(" ", 3);

		// If the split status line is not partitioned correctly
		if (splitStatusLine.length != 3) {
			logger.error("Status line has invalid number of values");
			return false;
		}

		// Get the status code from the status line
		int code = Integer.parseInt(splitStatusLine[1]);

		// Get the status object based on the code
		HttpStatusCode statusCode = HttpStatusCode.valueOf(code);

		// If the status code was not found
		if (statusCode == HttpStatusCode.NONE) {
			logger.error("Response Status Code is invalid");
			return false;
		}

		// Populate the response status code on the response object
		response.setStatus(statusCode);
		return true;
	}


	private static boolean populateHeaders(String headersLine, HttpResponse response) {
		// If there is no status line to parse
		if (headersLine == null || headersLine.isBlank()) {
			logger.error("Header line does not exist");
			return false;
		}

		// Use header utils to parse headers
		Optional<Map<String, String>> optionalHeaders = HttpHeaderUtils.parseHeaders(headersLine);

		// If the headers cannot be parsed
		if (optionalHeaders.isEmpty()) {
			logger.error("Headers cannot be parsed");
			return false;
		}

		// Get the headers
		Map<String, String> headers = optionalHeaders.get();

		// If there are no headers
		if (headers.isEmpty()) {
			logger.error("Parsed headers do not exist");
			return false;
		}

		// Populate the headers on the HTTP Response object
		response.setHeaders(headers);
		return true;
	}
}
