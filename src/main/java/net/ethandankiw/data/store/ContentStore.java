package net.ethandankiw.data.store;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.data.WeatherData;
import net.ethandankiw.data.http.JSON;

public class ContentStore {

	private static final Logger logger = LoggerFactory.getLogger(ContentStore.class);

	// Queue for incoming put requests, sorted by Lamport clock
	private static final PriorityBlockingQueue<ContentRequest> requestQueue = new PriorityBlockingQueue<>();

	// Using a ConcurrentHashMap for thread-safe access
	private static final ConcurrentHashMap<String, WeatherData> data = new ConcurrentHashMap<>();

	// A semaphore to control the rate of processing requests
	private static final Semaphore processPermit = new Semaphore(1);

	private static final int EXPIRY_SECONDS = 30;

	// Use a scheduled executor service for the expiration task
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


	private ContentStore() {
	}


	/**
	 * Adds a new request to the processing queue.
	 *
	 * @param id The station ID.
	 * @param json The JSON data to store.
	 * @param lamportClock The Lamport clock timestamp of the event.
	 */
	public static synchronized void put(String id, JSON json, long lamportClock) {
		requestQueue.add(new ContentRequest(id, json, lamportClock));
	}


	/**
	 * Gets the JSON data for a specific station ID.
	 *
	 * @param id The station ID.
	 * @return An Optional containing the JSON data, or an empty Optional if not found.
	 */
	public static synchronized JSON get(String id) {
		WeatherData weatherData = data.get(id);
		return weatherData != null ? weatherData.getJson() : null;
	}


	/**
	 * Check if JSON data for a specific station ID already exists.
	 *
	 * @param id The station ID.
	 * @return a boolean for if the data exists
	 */
	public static synchronized boolean exists(String id) {
		return data.containsKey(id);
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
		scheduler.scheduleAtFixedRate(ContentStore::removeExpired, 0, 10, TimeUnit.SECONDS);
		logger.info("Content store expiry task started. Will run every 10 seconds.");
	}


	/**
	 * Loads the content store from the persistence layer (disk). This method is designed to be called once on server
	 * startup.
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
	public static void shutdown() {
		scheduler.shutdown();
		logger.info("Content store expiry task shut down.");
	}

	/**
	 * Starts a background thread to process requests from the queue.
	 */
	public static void startProcessorThread() {
		Thread processorThread = new Thread(() -> {
			while (true) {
				try {
					ContentRequest request = requestQueue.take();
					// Acquire a permit to simulate a delay between processing requests
					processPermit.acquire();
					processRequest(request);
					processPermit.release();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					logger.error("Request processor thread interrupted.", e);
					break;
				}
			}
		}, "ContentStoreProcessor");
		processorThread.setDaemon(true);
		processorThread.start();
		logger.info("Content store processor thread started.");
	}

	private static void processRequest(ContentRequest request) {
		// This is where the old put logic goes
		WeatherData oldData = data.put(request.getId(), new WeatherData(request.getJson(), request.getLamportClock()));
		FileManager.saveContentStore();
		if (oldData != null) {
			logger.info("Content for ID {} updated. Lamport Clock: {}", request.getId(), request.getLamportClock());
		} else {
			logger.info("Content for ID {} created. Lamport Clock: {}", request.getId(), request.getLamportClock());
		}
	}
}
