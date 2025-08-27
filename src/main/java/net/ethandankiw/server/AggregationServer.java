package net.ethandankiw.server;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.HttpRequest;
import net.ethandankiw.utils.RequestUtils;

public class AggregationServer {

	private static final Logger logger = LoggerFactory.getLogger(AggregationServer.class);

	// Define a pool of threads to handle client requests
	ExecutorService pool = Executors.newFixedThreadPool(GlobalConstants.MAX_THREADS_FOR_CLIENT_REQUESTS);

	// Current number of threads that are active
	Integer activeThreads = 0;

	// Flag for if the server is accepting new requests
	boolean acceptingNewRequests = true;

	// Create a lock on the number of active request processing threads
	private final ReentrantLock lock = new ReentrantLock();

	// Create a queue of threads waiting for the lock to be released
	private final Condition drained = lock.newCondition();


	// Public constructor
	public AggregationServer() { /* Do nothing */ }


	// Get the current load of the server as a percentage
	Double getLoad() {
		// Get the lock on the active thread variable
		lock.lock();
		try {
			// Calculate the server load as a percentage out of 100
			return (double) activeThreads / (double) GlobalConstants.MAX_THREADS_FOR_CLIENT_REQUESTS;
		} finally {
			lock.unlock();
		}
	}

	// Stop accepting any new client connections
	public void startDraining() {
		acceptingNewRequests = false;
	}

	// Check if the server is accepting any new client requests
	public boolean isDraining() {
		return !acceptingNewRequests;
	}

	public void handleClientConnection(Socket client) {
		// If the server is no longer accepting requests
		if (isDraining()) {
			return;
		}

		// Spawn a new thread from the pool to process the client request
		logger.debug("Spawning new thread to handle connection");
		pool.submit(() -> handleConnection(client));

		// Increment the number of active request processing threads
		incrementActiveThreads();
	}


	private void handleConnection(Socket client) {
		// Parse a possible request from the client
		Optional<HttpRequest> optionalRequest = RequestUtils.parseClientRequest(client);

		// If the request was unable to be parsed
		if (optionalRequest.isEmpty()) {
			logger.error("Unable to parse client request");

			// Safely close the client connection
			closeClientConnection(client);
			return;
		}

		// Print that the request was parsed from the connection
		logger.debug("Client request has been parsed");

		// If request is valid
		HttpRequest request = optionalRequest.get();

		// Print that a new connection is being handled
		logger.debug("Client request is being handled");

		// Handle the request from the client
		// TODO: Send a response back to client
		RequestUtils.handleClientRequest(request);

		// Safely close the client connection
		closeClientConnection(client);

		// Decrement the number of active request processing threads
		decrementActiveThreads();
	}

	public void incrementActiveThreads() {
		// Get the lock on the active thread variable
		lock.lock();

		try {
			// Increment the active thread count
			activeThreads += 1;
		} finally {
			// Release the lock
			lock.unlock();
		}
	}

	public void decrementActiveThreads() {
		// Get the lock on the active thread variable
		lock.lock();

		try {
			// Decrement the active thread count
			activeThreads -= 1;

			// If there are no more request threads
			if (activeThreads == 0) {
				// Notify waiting threads that the server is drained
				drained.signalAll();
			}
		} finally {
			// Release the lock
			lock.unlock();
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
			while (activeThreads > 0) {
				// Wait for there to be no more processed threads
				drained.await();
			}
		} finally {
			lock.unlock();
		}
	}

	public void shutdown() {
		// Shutdown the thread pool
		pool.shutdown();
		logger.info("AggregationServer thread pool has shutdown successfully");
	}
}
