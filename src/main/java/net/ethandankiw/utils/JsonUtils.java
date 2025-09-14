package net.ethandankiw.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.data.http.JSON;

public class JsonUtils {

	public static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);


	private JsonUtils() {
	}


	public static String parseJSONToString(JSON json) {
		// Create a string build
		StringBuilder builder = new StringBuilder();

		// Add the opening brackets for JSON
		builder.append("{");

		try (Formatter formatter = new Formatter(builder, Locale.US)) {
			// Counters for entry count
			int current = 0;
			int total = json.get()
							.size();

			// For each of the key-value pairs
			for (Map.Entry<String, String> entry : json.get()
													   .entrySet()) {
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
		builder.append("}");

		// Build the string
		return builder.toString();
	}


	/**
	 * Reads a file that contains a single JSON object and parses it into a JSON object. This method is designed for
	 * files that are themselves valid JSON.
	 *
	 * @param file The file containing the JSON string.
	 * @return A JSON object with the parsed key-value pairs, or an empty JSON object if parsing fails.
	 */
	public static JSON parseJsonFileToJson(File file) {
		try {
			Path filePath = file.toPath();
			String fileContent = Files.readString(filePath);
			return parseStringToJSON(fileContent);
		} catch (IOException e) {
			logger.error("Failed to read file {} for JSON parsing: {}", file.getName(), e.getMessage());
			return new JSON();
		}
	}


	public static JSON parseTextFileToJSON(File file) {

		// Create a new JSON object
		JSON json = new JSON();

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
				json.add(keyValue.getKey(), keyValue.getValue());
			}

			// Close the scanner
			scanner.close();
		} catch (FileNotFoundException fnfe) {
			logger.warn("File does not exist: {}", fnfe.getMessage());
		}

		// Return the JSON key-value map
		return json;
	}


	public static JSON parseStringToJSON(String string) {

		// Create a new JSON object
		JSON json = new JSON();

		// Format the input data string
		String data = string.trim();

		// If the input string is empty or does not start with '{' and end with '}'
		if (data.isEmpty() || !(data.startsWith("{") && data.endsWith("}"))) {
			return json;
		}

		try {
			// Remove the curly braces
			String removedBraces = data.substring(1, data.length() - 2);

			// Remove quotes
			String removedQuotes = removedBraces.replaceAll("[\"']", "");

			// Split the string based on comma
			String[] split = removedQuotes.split(",");

			// For each key-value pair string
			for (String str : split) {
				// Split each key-value pair string
				String[] keyValueStr = str.split(":", 2);

				// Get the key value pair
				String key = keyValueStr[0].trim()
										   .toLowerCase();
				String value = keyValueStr[1].trim();

				// Put the key value on the JSON object
				json.add(key, value);
			}
		} catch (IndexOutOfBoundsException e) {
			logger.warn("Unable to substring: {}", e.getMessage());
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
