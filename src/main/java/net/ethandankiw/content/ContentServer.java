package net.ethandankiw.content;

import java.net.Socket;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

import org.jetbrains.annotations.Blocking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.LamportClock;
import net.ethandankiw.data.http.HttpRequest;
import net.ethandankiw.data.http.HttpRequestMethod;
import net.ethandankiw.data.http.HttpResponse;
import net.ethandankiw.data.http.JSON;
import net.ethandankiw.data.store.FileManager;
import net.ethandankiw.utils.JsonUtils;
import net.ethandankiw.utils.SocketUtils;
import net.ethandankiw.utils.UrlUtils;
import net.ethandankiw.utils.http.HttpResponseUtils;

public class ContentServer {

	public static final Logger logger = LoggerFactory.getLogger(ContentServer.class);

	// Define the lamport clock
	private static final LamportClock clock = new LamportClock();


	// PUBLIC VARIABLES USED IN JUNIT TESTING
	public static BlockingDeque<HttpResponse> storedResponses;
	public static HttpResponse storedResponse;


	public static void main(String[] args) {
		// Reset the stored request
		storedResponses = new LinkedBlockingDeque<>();
		storedResponse = null;

		// Check that the correct number of command line arguments were provided
		// Length = 1 for just URL
		// Length = 2 for URL + file location
		if (args.length != 2) {
			logger.error("Missing server url and file location");
			return;
		}

		// Parse the server URL
		String rawServerURL = args[0];

		// Build a URL to the station's weather data
		Optional<URI> optionalServerURL = UrlUtils.buildServerURL(rawServerURL);

		// If the URL to the server cannot be built
		if (optionalServerURL.isEmpty()) {
			logger.error("Unable to build server URL");
			return;
		}

		// Get the Server URL
		URI serverURL = optionalServerURL.get();

		// Get the Host and Port from the URL
		String host = serverURL.getHost();
		int port = serverURL.getPort();

		// Parse the station ID
		String rawFilePath = args[1];

		// Get the text file and parse to JSON at the file path
		Optional<JSON> optionalJSON = FileManager.readJSONFromFile(rawFilePath);

		// If the file does not exist, or cannot be parsed
		if (optionalJSON.isEmpty()) {
			logger.error("Unable to get JSON data at specified file path");
			return;
		}

		// Get the JSON weather data
		JSON json = optionalJSON.get();

		// Validate the parsed JSON weather data
		if (!json.containsKey("id")) {
			logger.error("JSON weather data is not valid as no ID is present");
			return;
		}

		// Tick the lamport clock for sending the request
		clock.tick();

		// Create a new HTTP request object to be sent to the aggregation server
		HttpRequest request = new HttpRequest();

		// Populate the request with data
		request.setMethod(HttpRequestMethod.PUT);
		request.setPath("/weather.json");
		request.setVersion("HTTP/1.1");

		// Populate the headers
		request.addHeader("User-Agent", "ATOMClient/1/0");
		request.addHeader("Content-Type", "application/json");

		// Put the lamport clock value in the header
		long clockValue = clock.getClockValue();
		request.addHeader(GlobalConstants.LAMPORT_CLOCK_HEADER, String.valueOf(clockValue));

		// Parse the JSON into a string
		String jsonData = JsonUtils.parseJSONToString(json);

		// Calculate the size of the JSON data for content length
		Integer size = jsonData.length();

		// Populate the content length header
		request.addHeader("Content-Length", String.valueOf(size));

		// Populate the body of the request with the JSON data
		request.setBody(jsonData);

		// Open a socket connection to the content server
		Optional<Socket> optionalServerConnection = SocketUtils.createClientSocket(host, port);

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

		// Parse the response from the server
		Optional<HttpResponse> optionalResponse = HttpResponseUtils.parseServerResponse(serverConnection);

		// If there is no response
		if (optionalResponse.isEmpty()) {
			logger.error("Response from {} does not exist", serverConnection.getInetAddress());
			return;
		}

		// Get the response
		HttpResponse response = optionalResponse.get();

		// Store the response
		storedResponses.add(response);
		storedResponse = response;

		// Print the response
		String responseString = response.toString();
		logger.debug("\n\n{}\n\n", responseString);
	}
}
