package net.ethandankiw.client;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.http.HttpRequest;
import net.ethandankiw.data.http.HttpRequestMethod;
import net.ethandankiw.data.http.HttpResponse;
import net.ethandankiw.data.http.HttpStatusCode;
import net.ethandankiw.utils.SocketUtils;
import net.ethandankiw.utils.http.HttpResponseUtils;

public class GetClient {

	public static final Logger logger = LoggerFactory.getLogger(GetClient.class);

	// Define the maximum number of attempted requests the GET client can attempt to the server before failing
	private static final Integer MAX_ATTEMPTS = 5;

	// Define the starting delay for failed request (grows with exponential backoff)
	private static final Integer STARTING_DELAY = 1; // seconds

	// Define teh starting timeout for how long the GET client should wait
	// after sending a request before determining it as failed on duration alone
	private static final Integer STARTING_TIMEOUT = 5; // seconds


	public static void main(String[] args) {
		// Create a new HTTP request object to be sent to the aggregation server
		HttpRequest request = new HttpRequest();

		// Populate the request with data
		request.setMethod(HttpRequestMethod.GET);
		request.setPath("/weather.json");

		// Populate the headers
		request.addHeader("User-Agent", "ATOMClient/1/0");
		request.addHeader("Content-Type", "application/json");

		// Initialise the lamport clock to 0
		request.addHeader(GlobalConstants.LAMPORT_CLOCK_HEADER, String.valueOf(0));

		// Loop to retry until a successful response
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			// Log an attempt is being made
			logger.debug("Attempting to send request to the server: {}/{}", attempt, MAX_ATTEMPTS);

			// Attempt to make a request to the server
			Optional<HttpResponse> optionalResponse = attemptRequest(request, attempt);

			// If there is a response from the server
			if (optionalResponse.isPresent()) {
				// Get the response
				HttpResponse response = optionalResponse.get();

				// Get the response status code
				Integer statusCode = response.getStatusCode();

				// If the response is successful
				if (HttpStatusCode.isSuccess(statusCode)) {
					logger.info("Successful response received");
					// No need to retry a successful response
					return;
				}

				// If the response is not successful and cant be retried
				if (!HttpStatusCode.isRetryable(statusCode)) {
					logger.error("Unsuccessful response cannot be retried: {}", statusCode);
					return;
				}
			}

			// If there is no response from the server
			// Or there is a response that can be retried

			// Check if there are no more retries allowed
			if (attempt >= MAX_ATTEMPTS) {
				break;
			}

			// If there is an available retry

			// Calculate how long the GET client should wait before retrying
			long delay = (long) (STARTING_DELAY * Math.pow(2, attempt - 1d));
			logger.info("Retrying request in {} seconds...", delay);

			// Delay the response to allow the server to recover
			try {
				TimeUnit.SECONDS.sleep(delay);
			} catch (InterruptedException e) {
				Thread.currentThread()
					  .interrupt();
				logger.error("Thread interrupted during sleep", e);
				return;
			}
		}

		// If all attempts were exhausted
		logger.error("All {} request attempts were made, request failing.", MAX_ATTEMPTS);
	}


	private static Optional<HttpResponse> attemptRequest(HttpRequest request, int attempt) {
		// Open a socket connection to the content server
		Optional<Socket> optionalServerConnection = SocketUtils.createClientSocket(GlobalConstants.SERVER_IP, GlobalConstants.SERVER_PORT);

		// If the connection doesn't exist
		if (optionalServerConnection.isEmpty()) {
			logger.error("Unable to make a connection to the server");
			return Optional.empty();
		}

		// Get the client -> server socket connection
		try (Socket serverConnection = optionalServerConnection.get()) {
			// Calculate how long the GET client should wait for the timeout
			// With exponential backoff
			int timeout = (int) (STARTING_TIMEOUT * Math.pow(2, attempt - 1d));

			// Define the request timeout on the socket
			serverConnection.setSoTimeout(timeout * 1000);

			// Make the request by writing to the client -> server socket
			boolean success = SocketUtils.writeToSocket(serverConnection, request.toString());

			// If the request was not successful
			if (!success) {
				logger.error("Request to server {} was unsuccessful", serverConnection.getInetAddress());
				return Optional.empty();
			}

			// Parse and return the response from the server
			return HttpResponseUtils.parseServerResponse(serverConnection);
		} catch (IOException ioe) {
			logger.error("Error occurred when closing connection to the server");
		}

		// Default to no response if an error occurs
		return Optional.empty();
	}
}
