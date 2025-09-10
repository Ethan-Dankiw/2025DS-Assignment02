package net.ethandankiw.aggregation;

import java.io.IOException;
import java.net.Socket;
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
import net.ethandankiw.data.HttpRequest;
import net.ethandankiw.utils.RequestUtils;
import net.ethandankiw.utils.UuidUtils;

public class AggregationServer {

	private static final Logger logger = LoggerFactory.getLogger(AggregationServer.class);

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


	public void handleClientConnection(Socket client) {
		// If the server is no longer accepting requests
		if (isDraining()) {
			return;
		}

		// Increment the number of active request processing threads
		incrementActiveThreads();

		try {
			// Spawn a new thread from the pool to process the client request
			pool.submit(() -> {
				try {
					handleConnection(client);
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
			Optional<HttpRequest> optionalRequest = RequestUtils.parseClientRequest(client);

			// If the request was unable to be parsed
			if (optionalRequest.isEmpty()) {
				logger.error("Unable to parse client request");
				return;
			}

			// If request is valid
			HttpRequest request = optionalRequest.get();

			// Handle the request from the client
			// TODO: Send a response back to client
			RequestUtils.handleClientRequest(request);
		} catch (Exception e) {
			logger.error("Error handling client request: {}", e.getMessage());
		} finally {
			// Safely close the client connection
			closeClientConnection(client);
		}
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
