package net.ethandankiw.client;

import java.net.Socket;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.http.HttpRequest;
import net.ethandankiw.data.http.HttpRequestMethod;
import net.ethandankiw.utils.SocketUtils;

public class GetClient {

	public static final Logger logger = LoggerFactory.getLogger(GetClient.class);

	// Define the maximum number of retries the GET client can attempt to make to the server before failing
	private static final Integer MAX_RETRIES = 5;

	// Define the starting delay for failed request (grows with exponential backoff)
	private static final Integer STARTING_DELAY = 1; // seconds

	// Define how long should the GET client wait before determining a request as failed
	private static final Integer TIMEOUT = 5; // seconds

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

		// Open a socket connection to the content server
		Optional<Socket> optionalServerConnection = SocketUtils.createClientSocket(GlobalConstants.SERVER_IP, GlobalConstants.SERVER_PORT);

		// If the connection doesn't exist
		if (optionalServerConnection.isEmpty()) {
			logger.error("Unable to make a connection to the server");
			return;
		}

		// Get the client -> server socket connection
		Socket serverConnection = optionalServerConnection.get();

		// Make the request by writing to the client -> server socket
		boolean success = SocketUtils.writeToSocket(serverConnection, request.toString());

		// If the request was not successful
		if (!success) {
			logger.error("Request to {} was unsuccessful", serverConnection.getInetAddress());
			return;
		}

		// Read the response from the server
		Optional<String> optionalResponse = SocketUtils.readFromSocket(serverConnection);

		// If there was no response
		if (optionalResponse.isEmpty()) {
			logger.error("Response from server does not exist");
			return;
		}

		// Get the response
		String responseStr = optionalResponse.get();

		// If the response has no content
		if (responseStr.isEmpty()) {
			logger.error("Response from server was empty");
		}
	}
}
