package net.ethandankiw.data.store;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.data.http.JSON;
import net.ethandankiw.utils.JsonUtils;

public class FileManager {

	public static final Logger logger = LoggerFactory.getLogger(FileManager.class);

	// Path to where files are stored
	private static final String WEATHER_DATA_DIR = "src/main/java/net/ethandankiw/data/store/files/weatherdata";
	private static final String CONTENT_STORE_DIR = "src/main/java/net/ethandankiw/data/store/files/contentstore";
	private static final String CONTENT_STORE_DATA_EXTENSION = ".json";


	private FileManager() {
	}


	/**
	 * Saves all data from the ContentStore to individual files in the storage
	 * directory. Each file is named after the weather station's ID and contains
	 * the JSON data.
	 */
	public static void saveContentStore() {
		File dir = new File(CONTENT_STORE_DIR);
		if (!dir.exists() && !dir.mkdirs()) {
			logger.error("Failed to create storage directory: {}", CONTENT_STORE_DIR);
			return;
		}

		Map<String, JSON> allData = ContentStore.getAll();
		allData.forEach((id, json) -> {
			File file = new File(dir, id + CONTENT_STORE_DATA_EXTENSION);
			try (FileWriter writer = new FileWriter(file)) {
				writer.write(JsonUtils.parseJSONToString(json));
				logger.info("Saved weather data for ID {} to file.", id);
			} catch (IOException e) {
				logger.error("Failed to save data for ID {}: {}", id, e.getMessage());
			}
		});
	}


	/**
	 * Deletes a specific weather data file from the content store.
	 *
	 * @param id The ID of the weather station whose data file should be
	 * deleted.
	 */
	public static void deleteContentFile(String id) {
		File file = new File(CONTENT_STORE_DIR,
				id + CONTENT_STORE_DATA_EXTENSION);
		if (file.exists()) {
			if (file.delete()) {
				logger.info("Deleted expired weather data file for ID: {}", id);
			} else {
				logger.error("Failed to delete expired weather data file for ID: {}", id);
			}
		}
	}


	/**
	 * Loads all JSON files from the storage directory and populates the
	 * ContentStore. This is used for server recovery after a crash.
	 */
	public static void loadContentStore() {
		File dir = new File(CONTENT_STORE_DIR);
		if (!dir.exists()) {
			logger.warn("Storage directory does not exist. No data to load.");
			return;
		}

		File[] files = dir.listFiles((d, name) -> name.endsWith(CONTENT_STORE_DATA_EXTENSION));
		if (files == null)
			return;

		Arrays.stream(files)
			  .forEach(file -> {
				  try {
					  // Attempt to parse the file
					  JSON json = JsonUtils.parseJsonFileToJson(file);

					  // Check if the JSON is valid and contains an ID
					  if (json.containsKey("id")) {
						  // Put the data into the ContentStore
						  ContentStore.put(json.getValue("id"), json, 0);
						  logger.info("Loaded weather data for ID {} from file.", json.getValue("id"));
					  } else {
						  logger.warn("Skipping file '{}' due to invalid JSON or missing 'id'.", file.getName());
					  }
				  } catch (Exception e) {
					  logger.warn("Skipping file '{}' due to a parsing error: {}", file.getName(), e.getMessage());
				  }
			  });
	}


	public static List<File> getListOfStoredFiles() {
		// Get the file directory for the stored files
		File dir = new File(WEATHER_DATA_DIR);

		// If the directory does not exist
		if (!dir.exists()) {
			logger.warn("Storage directory does not exist");
			return List.of();
		}

		// If the directory is not a directory
		if (!dir.isDirectory()) {
			logger.warn("Storage directory exists, but is not a directory");
			return List.of();
		}

		// Get all the files in the dir
		File[] files = dir.listFiles();

		// If there are no files
		if (files == null || files.length == 0) {
			return List.of();
		}

		// Return the list of files
		return Arrays.asList(files);
	}


	public static Optional<JSON> readJSONFromFile(String path) {
		// Open the file at the path
		File file = new File(WEATHER_DATA_DIR, path);

		// If the file does not exist
		if (!file.exists()) {
			logger.warn("JSON file does not exist");
			return Optional.empty();
		}

		// If the file is not a file
		if (!file.isFile()) {
			logger.warn("JSON file exists, but is not a file");
			return Optional.empty();
		}

		// Parse the file into JSON
		return Optional.of(JsonUtils.parseTextFileToJSON(file));
	}
}
