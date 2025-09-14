package net.ethandankiw.aggregation;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.LamportClock;
import net.ethandankiw.data.http.HttpRequest;
import net.ethandankiw.data.http.HttpRequestMethod;
import net.ethandankiw.data.http.HttpStatusCode;
import net.ethandankiw.data.http.JSON;
import net.ethandankiw.data.store.ContentStore;
import net.ethandankiw.data.store.FileManager;
import net.ethandankiw.utils.JsonUtils;
import net.ethandankiw.utils.UuidUtils;
import net.ethandankiw.utils.http.HttpRequestUtils;
import net.ethandankiw.utils.http.HttpResponseUtils;

public class AggregationServer {

	private static final Logger logger = LoggerFactory.getLogger(AggregationServer.class);

	// Lamport clock for determining the order of received requests
	private final LamportClock clock = new LamportClock();

	// Unique ID for the aggregation server
	private final String uuid;

	// Define a pool of threads to handle client requests
	private final ExecutorService pool = Executors.newFixedThreadPool(GlobalConstants.MAX_THREADS_FOR_CLIENT_REQUESTS);

	// Create a lock on the number of active request processing threads
	private final ReentrantLock lock = new ReentrantLock();

	// Create a queue of threads waiting for the lock to be released
	private final Condition drained = lock.newCondition();

	// Current number of threads that are active
	private final AtomicInteger activeThreads = new AtomicInteger(0);

	// Flag for if the server is accepting new requests
	private volatile boolean acceptingNewRequests = true;


	// Public constructor
	public AggregationServer() {
		// Generate a unique ID
		uuid = UuidUtils.generateUUID();
	}


	public String getUUID() {
		return uuid;
	}


	// Get the current load of the server as a percentage
	public Double getLoad() {
		// Calculate the server load as a percentage out of 100
		return (double) getActiveRequestsCount() / (double) GlobalConstants.MAX_THREADS_FOR_CLIENT_REQUESTS;
	}


	// Get the number of active requests
	public Integer getActiveRequestsCount() {
		// Get the number of active threads that are processing requests
		return activeThreads.get();
	}


	// Stop accepting any new client connections
	public void startDraining() {
		acceptingNewRequests = false;
	}


	// Check if the server is accepting any new client requests
	public boolean isDraining() {
		return !acceptingNewRequests;
	}


	// Check if the server is at capacity
	public boolean atCapacity() {
		// If the server is at capacity
		return getLoad() >= 1;
	}


	public void handleClientConnection(Socket client, LamportClock lbClock) {
		// If the server is no longer accepting requests
		if (isDraining()) {
			return;
		}

		// Receive clock value from load balancer
		clock.receive(lbClock.getClockValue());

		// Increment the number of active request processing threads
		incrementActiveThreads();

		try {
			// Spawn a new thread from the pool to process the client request
			pool.submit(() -> {
				try {
					// Process the client request
					handleConnection(client);

					// Update the load balancer clock value
					lbClock.receive(clock.getClockValue());


				} catch (Exception e) {
					logger.error("Error occurred while handling client connection: {}", e.getMessage());
				} finally {
					decrementActiveThreads();
				}
			});
		} catch (Exception e) {
			logger.error("Error occurred while submitting task for execution: {}", e.getMessage());

		}
	}


	private void handleConnection(Socket client) {
		try {
			// Parse a possible request from the client
			Optional<HttpRequest> optionalRequest = HttpRequestUtils.parseClientRequest(client);

			// If the request was unable to be parsed
			if (optionalRequest.isEmpty()) {
				logger.error("Unable to parse client request");
				String responseBody = "Unable to parse client request";
				HttpResponseUtils.generateAndSendResponse(client, HttpStatusCode.BAD_REQUEST, responseBody, clock.getClockValue());
				return;
			}

			// If request is valid
			HttpRequest request = optionalRequest.get();

			// Get the request method
			HttpRequestMethod method = request.getMethod();

			// Handle the request according to the method
			switch (method) {
				case GET -> {
					logger.debug("Detected GET request");
					handleGETRequest(client);
				}
				case PUT -> {
					logger.debug("Detected PUT request");
					handlePUTRequest(client, request);
				}
				case null, default -> {
					logger.error("Invalid request method: {}", method);
					String responseBody = "Invalid request method";
					HttpResponseUtils.generateAndSendResponse(client, HttpStatusCode.BAD_REQUEST, responseBody, clock.getClockValue());
				}
			}
		} catch (Exception e) {
			logger.error("Error handling client request: {}", e.getMessage());
			String responseBody = "Error handling client request";
			HttpResponseUtils.generateAndSendResponse(client, HttpStatusCode.INTERNAL_SERVER_ERROR, responseBody, clock.getClockValue());
		} finally {
			// Safely close the client connection
			closeClientConnection(client);
		}
	}


	private void handlePUTRequest(Socket client, HttpRequest request) {
		// Get the request body
		String body = request.getBody();

		// Check for no content
		if (body == null || body.trim()
								.isEmpty()) {
			HttpResponseUtils.generateAndSendResponse(client, HttpStatusCode.NO_CONTENT, "No content provided in PUT request.", clock.getClockValue());
			return;
		}

		// Parse the request body to JSON
		JSON json = JsonUtils.parseStringToJSON(body);

		// Check if the JSON is valid and contains an ID
		if (!json.containsKey("id")) {
			HttpResponseUtils.generateAndSendResponse(client, HttpStatusCode.BAD_REQUEST, "Invalid JSON data or missing 'id' key.", clock.getClockValue());
			return;
		}

		String id = json.getValue("id");

		// Causal event for successful PUT to content store
		// Get the lamport clock from the request
		String valueStr = request.getHeaderValue(GlobalConstants.LAMPORT_CLOCK_HEADER);
		long value = valueStr == null ? 0 : Long.parseLong(valueStr);
		// Update the lamport port request according to the received value
		clock.receive(value);

		// Check if the to be inserted json already exists
		boolean exists = ContentStore.exists(id);

		// Store the JSON object in the content store
		ContentStore.put(id, json, clock.getClockValue());

		// Persist the updated content store
		FileManager.saveContentStore();

		// If the data did not exist before putting in the content store
		if (!exists) {
			HttpResponseUtils.generateAndSendResponse(client, HttpStatusCode.CREATED,
					"Content for ID " + id + " created.", clock.getClockValue());
			return;
		}

		// If the data did exist
		HttpResponseUtils.generateAndSendResponse(client, HttpStatusCode.OK,
				"Content for ID " + id + " updated.", clock.getClockValue());

	}


	private void handleGETRequest(Socket client) {
		// Get all data from the content store
		Map<String, JSON> allData = ContentStore.getAll();

		// Check if there is no content to return
		if (allData.isEmpty()) {
			HttpResponseUtils.generateAndSendResponse(client, HttpStatusCode.NO_CONTENT, "No weather data available.", clock.getClockValue());
			return;
		}

		// Causal event for successful GET from content store
		clock.tick();

		// Create a single JSON object to hold the aggregated data
		JSON aggregatedJSON = new JSON();
		allData.forEach((id, json) -> aggregatedJSON.add(id, JsonUtils.parseJSONToString(json)));

		// Convert the aggregated JSON to a string
		String responseBody = JsonUtils.parseJSONToString(aggregatedJSON);

		// Send the aggregated JSON back to the client
		HttpResponseUtils.generateAndSendResponse(client, HttpStatusCode.OK, responseBody, clock.getClockValue());
	}


	public void incrementActiveThreads() {
		// Increment the number of active threads that are processing requests
		activeThreads.incrementAndGet();
	}


	public void decrementActiveThreads() {
		// Decrement the number of active threads that are processing requests
		int count = activeThreads.decrementAndGet();

		// If done processing
		if (count == 0) {
			// Acquire the lock on the drained condition
			lock.lock();

			try {
				// Signal that the server is done processing
				drained.signalAll();
			} finally {
				// Release the lock
				lock.unlock();
			}
		}
	}


	public void closeClientConnection(Socket client) {
		try {
			logger.info("Closing client connection socket");
			client.close();
			logger.info("Client connection closed");
		} catch (IOException ioe) {
			logger.error("Unable to close client connection: {}", ioe.getMessage());
		}
	}


	public void awaitFinishedProcessing() throws InterruptedException {
		// Get the lock on the active threads variable
		lock.lock();

		try {
			// While there are requests being processed
			while (getActiveRequestsCount() > 0) {
				// Wait for there to be no more processed threads
				drained.await();
			}
		} finally {
			lock.unlock();
		}
	}


	public void shutdown() {
		// Start the shutdown process for the thread pool
		pool.shutdown();

		try {
			// Wait for the pool to shut down
			boolean success = pool.awaitTermination(5, TimeUnit.SECONDS);

			// If the shutdown was not successful
			if (!success) {
				logger.warn("Timeout elapsed while shutting down thread pool");
				return;
			}

			// If the shutdown was successful
			logger.debug("Thread pool has shutdown successfully");
		} catch (InterruptedException ie) {
			logger.error("Error occurred while shutting down thread pool");
		}
	}
}
