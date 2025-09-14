package net.ethandankiw.content;

import java.io.File;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.http.HttpRequest;
import net.ethandankiw.data.http.HttpRequestMethod;
import net.ethandankiw.data.http.JSON;
import net.ethandankiw.data.store.FileManager;
import net.ethandankiw.utils.JsonUtils;
import net.ethandankiw.utils.SocketUtils;

public class ContentServer {

	public static final Logger logger = LoggerFactory.getLogger(ContentServer.class);


	public static void main(String[] args) {

		// Get a list of files in the storage directory
		List<File> files = FileManager.getListOfStoredFiles();

		// For each of the files
		for (File file : files) {
			// Parse the file to JSON
			JSON jsonWeatherData = JsonUtils.parseTextFileToJSON(file);

			// Validate the parsed JSON weather data
			if (!jsonWeatherData.containsKey("id")) {
				continue;
			}

			// Create a new HTTP request object to be sent to the aggregation server
			HttpRequest request = new HttpRequest();

			// Populate the request with data
			request.setMethod(HttpRequestMethod.PUT);
			request.setPath("/weather.json");
			request.setVersion("HTTP/1.1");

			// Populate the headers
			request.addHeader("User-Agent", "ATOMClient/1/0");
			request.addHeader("Content-Type", "application/json");

			// Initialise the lamport clock to 0
			request.addHeader(GlobalConstants.LAMPORT_CLOCK_HEADER, String.valueOf(0));

			// Parse the JSON into a string
			String jsonData = JsonUtils.parseJSONToString(jsonWeatherData);

			// Calculate the size of the JSON data for content length
			Integer size = jsonData.length();

			// Populate the content length header
			request.addHeader("Content-Length", String.valueOf(size));

			// Populate the body of the request with the JSON data
			request.setBody(jsonData);

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
				return;
			}
		}
	}
}
