package net.ethandankiw.utils.http;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HttpHeaderUtils {

	private HttpHeaderUtils() {
	}


	public static Optional<Map<String, String>> parseHeaders(String allHeaders) {
		// If there are no headers to parse
		if (allHeaders.isBlank()) {
			return Optional.empty();
		}

		// Define the map of header key values paris
		Map<String, String> headers = new HashMap<>();

		// Split the headers
		String[] splitHeaders = allHeaders.split("\r\n");

		// Parse all the headers
		for (String header : splitHeaders) {
			// If the header line is empty
			if (header.isBlank()) {
				continue;
			}

			// Parse the header line
			Map.Entry<String, String> keyValue = parseSingleHeader(header);

			// Add the header key and value to the map
			headers.put(keyValue.getKey(), keyValue.getValue());
		}

		// Return the parsed headers
		return Optional.of(headers);
	}


	// https://www.baeldung.com/java-pairs
	private static AbstractMap.SimpleEntry<String, String> parseSingleHeader(String headerLine) {
		// Split on first colon
		String[] headerKeyValue = headerLine.split(":", 2);

		// Remove whitespace for each value
		String key = headerKeyValue[0].trim()
									  .toLowerCase();
		String value = headerKeyValue[1].trim();

		// Return a key value pair for the header
		return new AbstractMap.SimpleEntry<>(key, value);
	}
}
