package net.ethandankiw.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.AbstractMap;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtils {

	public static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);


	private JsonUtils() {
	}


	public static String parseToString(Map<String, String> json) {
		// Create a string build
		StringBuilder builder = new StringBuilder();

		// Add the opening brackets for JSON
		builder.append("{");

		try (Formatter formatter = new Formatter(builder, Locale.US)) {
			// Counters for entry count
			int current = 0;
			int total = json.size();

			// For each of the key-value pairs
			for (Map.Entry<String, String> entry : json.entrySet()) {
				formatter.format("\"%s\": \"%s\"", entry.getKey(), entry.getValue());
				// If it's not the last entry, add a comma
				if (++current < total) {
					builder.append(",");
				}
			}
		} catch (Exception e) {
			logger.error("Unable to parse JSON: {}", e.getMessage());
		}

		// Add the closing brackets for JSON
		builder.append("{");

		// Build the string
		return builder.toString();
	}


	public static Map<String, String> parseToJSON(File file) {

		// Create a map of key value pairs from the File
		Map<String, String> json = new HashMap<>();

		try {
			// Use a scanner to read text files
			Scanner scanner = new Scanner(file);

			// Read lines from the file
			while (scanner.hasNextLine()) {
				// Get the line from the file
				String line = scanner.nextLine();
				// Parse the line into key-value pairs
				AbstractMap.SimpleEntry<String, String> keyValue = parseKeyValue(line);
				// Put the key value into the json map
				json.put(keyValue.getKey(), keyValue.getValue());
			}

			// Close the scanner
			scanner.close();
		} catch (FileNotFoundException fnfe) {
			logger.warn("File does not exist: {}", fnfe.getMessage());
		}

		// Return the JSON key-value map
		return json;
	}


	private static AbstractMap.SimpleEntry<String, String> parseKeyValue(@NotNull String line) {
		// Split the line based on the colon separator
		String[] values = line.split(":", 2);

		// Get the key and value
		String key = values[0].trim();
		String value = values[1].trim();

		// Return the key-value pair
		return new AbstractMap.SimpleEntry<>(key, value);
	}
}
