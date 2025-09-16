package net.ethandankiw.data.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.data.LamportClock;
import net.ethandankiw.data.WeatherData;
import net.ethandankiw.data.http.HttpRequestMethod;
import net.ethandankiw.data.http.JSON;

public class ContentStore {

	private static final Logger logger = LoggerFactory.getLogger(ContentStore.class);
	private static final int EXPIRY_SECONDS = 30;
	// Queue for incoming put requests, sorted by Lamport clock
	private static PriorityBlockingQueue<ContentRequest> requestQueue;
	// Using a ConcurrentHashMap for thread-safe access
	private static ConcurrentHashMap<String, WeatherData> data;
	// A semaphore to control the rate of processing requests
	private static Semaphore processPermit;
	// Use a scheduled executor service for the expiration task
	private static ScheduledExecutorService scheduler;

	// A thread to process requests to the content store
	private static Thread processorThread = null;

	// Define the lamport clock
	private static final LamportClock clock = new LamportClock();


	private ContentStore() {
	}


	/**
	 * Adds a new request to the processing queue.
	 *
	 * @param id The station ID.
	 * @param json The JSON data to store.
	 * @param lamportClock The Lamport clock timestamp of the event.
	 * @return boolean for if a new value is created
	 */
	public static synchronized boolean put(String id, JSON json, long lamportClock) {
		// Receive a lamport clock value
		receiveClock(lamportClock);

		// If the request already exists
		boolean exists = exists(id);
		requestQueue.add(new ContentRequest(id, json, clock.getClockValue()));
		logger.info("PUT request queued for ID: {}, queue length {}, clock "
				+ "{}", id, data.size(), clock.getClockValue());
		return !exists;
	}


	/**
	 * Gets the JSON data for a specific station ID.
	 *
	 * @param id The station ID.
	 * @return An Optional containing the JSON data, or an empty Optional if not
	 * found.
	 */
	public static synchronized CompletableFuture<JSON> get(String id, long lamportClock) {
		// Receive a lamport clock value
		receiveClock(lamportClock);

		// Create a future to hold the result
		CompletableFuture<JSON> future = new CompletableFuture<>();
		// Create a GET request object and add it to the queue
		requestQueue.add(new ContentRequest(id, future, clock.getClockValue()));
		logger.info("GET request queued for ID: {}, queue length {}, clock "
				+ "{}", id, data.size(), clock.getClockValue());
		return future;
	}


	public static synchronized void receiveClock(long clockValue) {
		clock.receive(clockValue);
	}


	/**
	 * Check if JSON data for a specific station ID already exists.
	 *
	 * @param id The station ID.
	 * @return a boolean for if the data exists
	 */
	public static synchronized boolean exists(String id) {
		// If the request is stored
		if (data.containsKey(id)) {
			return true;
		}

		// If the request is in the queue
		AtomicBoolean found = new AtomicBoolean(false);
		requestQueue.forEach(req -> {
			if (Objects.equals(req.getId(), id)) {
				found.set(true);
			}
		});

		// Return if the request was found in the queue
		return found.get();
	}


	/**
	 * Gets a copy of all weather data.
	 *
	 * @return A new Map containing all valid weather data.
	 */
	public static synchronized Map<String, JSON> getAll() {
		Map<String, JSON> allData = new HashMap<>();
		data.forEach((id, weatherData) -> allData.put(id, weatherData.getJson()));
		return allData;
	}


	/**
	 * Starts a background task to remove expired content.
	 */
	public static void startExpiryTask() {
		scheduler.scheduleAtFixedRate(ContentStore::removeExpired, 0, 30, TimeUnit.SECONDS);
		logger.info("Content store expiry task started. Will run every 10 seconds.");
	}


	/**
	 * Loads the content store from the persistence layer (disk). This method is
	 * designed to be called once on server startup.
	 */
	public static void loadFromDisk() {
		FileManager.loadContentStore();
		logger.info("Content store initialized from disk.");
	}


	/**
	 * A private method to check for and remove expired content.
	 */
	private static void removeExpired() {
		long now = System.currentTimeMillis();
		data.forEach((id, weatherData) -> {
			if (now - weatherData.getLastUpdated() > EXPIRY_SECONDS * 1000) {
				FileManager.deleteContentFile(id);
				data.remove(id);
				logger.info("Removed expired data for station ID: {}", id);
			}
		});
	}


	/**
	 * Shuts down the expiry task scheduler.
	 */
	public static void stopExpiryTask() {
		scheduler.shutdown();
		logger.info("Content store expiry task shut down.");
	}


	/**
	 * Starts a background thread to process requests from the queue.
	 */
	public static void startProcessorThread() {
		// If the thread is already running
		if (processorThread != null) {
			logger.error("Unable to start processor thread as it's already running");
			return;
		}

		processorThread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(1000);
					ContentRequest request = requestQueue.take();
					// Acquire a permit to simulate a delay between processing requests
					processPermit.acquire();
					logger.debug("Processing Request");
					processRequest(request);
					processPermit.release();
				} catch (InterruptedException e) {
					Thread.currentThread()
						  .interrupt();
					break;
				}
			}
		}, "ContentStoreProcessor");
		processorThread.setDaemon(true);
		processorThread.start();
		logger.info("Content store processor thread started.");
	}


	public static void stopProcessorThread() {
		// If the thread is not running
		if (processorThread == null) {
			logger.error("Unable to stop processor thread as it's not running");
			return;
		}

		// Interrupt the processor thread
		processorThread.interrupt();

		try {
			// Wait for the thread to die gracefully
			processorThread.join(2000);
		} catch (InterruptedException e) {
			logger.warn("Interrupted while waiting for processor thread to stop.", e);
			return;
		}

		// Update the processor thread to not exist
		processorThread = null;

		logger.info("Content store processor thread stopped.");
	}


	private static void processRequest(ContentRequest request) {
		switch (request.getMethod()) {
			case HttpRequestMethod.PUT:
				logger.debug("Processing PUT Request");
				processPutRequest(request);
				break;
			case HttpRequestMethod.GET:
				logger.debug("Processing GET Request");
				processGetRequest(request);
				break;
		}
	}


	private static void processPutRequest(ContentRequest request) {
		WeatherData oldData = data.put(request.getId(), new WeatherData(request.getJson(), request.getLamportClock()));
		FileManager.saveContentStore();
		if (oldData != null) {
			logger.info("Content for ID {} updated. Lamport Clock: {}", request.getId(), request.getLamportClock());
		} else {
			logger.info("Content for ID {} created. Lamport Clock: {}", request.getId(), request.getLamportClock());
		}
	}


	private static void processGetRequest(ContentRequest request) {
		JSON result =
				data.get(request.getId()) != null ? data.get(request.getId())
														.getJson() : null;
		// Complete the future, giving the result back to the caller
		request.getFuture()
			   .complete(result);
		logger.info("GET request for ID {} completed.", request.getId());
	}


	public static void init() {
		reset();
	}


	public static void reset() {
		processPermit = new Semaphore(1);
		requestQueue = new PriorityBlockingQueue<>();
		data = new ConcurrentHashMap<>();
		scheduler = Executors.newSingleThreadScheduledExecutor();
	}
}
