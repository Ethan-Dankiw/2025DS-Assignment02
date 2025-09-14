package net.ethandankiw.data.store;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.data.WeatherData;
import net.ethandankiw.data.http.JSON;

public class ContentStore {

	private static final Logger logger = LoggerFactory.getLogger(ContentStore.class);

	// Using a ConcurrentHashMap for thread-safe access
	private static final ConcurrentHashMap<String, WeatherData> data = new ConcurrentHashMap<>();

	private static final int EXPIRY_SECONDS = 30;

	// Use a scheduled executor service for the expiration task
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


	private ContentStore() {
	}


	/**
	 * Puts or updates weather data for a specific station ID.
	 *
	 * @param id The station ID.
	 * @param json The JSON data to store.
	 * @return The previous JSON object for the given ID, or null if it was a new entry.
	 */
	public static synchronized JSON put(String id, JSON json) {

		WeatherData oldData = data.put(id, new WeatherData(json));
		if (oldData != null) {
			return oldData.getJson();
		}
		return null;
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
}
