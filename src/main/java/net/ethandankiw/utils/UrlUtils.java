package net.ethandankiw.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlUtils {

	public static final Logger logger = LoggerFactory.getLogger(UrlUtils.class);


	private UrlUtils() {
	}


	public static Optional<URI> buildServerURL(String rawServerURL) {
		return buildServerURL(rawServerURL, null);
	}


	public static Optional<URI> buildServerURL(String rawServerURL, @Nullable String stationID) {
		// Define a candidate for the Server URL
		String candidate = rawServerURL;

		// If the URL does not start with http://
		if (!candidate.startsWith("http://")) {
			candidate = "http://" + candidate;
		}

		try {
			// Construct a base URL from the raw Server URL candidate
			URI base = new URI(candidate);

			// Define the path to get data from
			String path = "/weather";

			// Define an optional station ID query
			String query = null;

			// If a station ID is provided
			if (stationID != null && !stationID.isBlank()) {
				query = "station="
						+ URLEncoder.encode(stationID, StandardCharsets.UTF_8);
			}

			// Define the full URL to the server
			return Optional.of(new URI(base.getScheme(), base.getUserInfo(), base.getHost(), base.getPort(), path, query, null));
		} catch (URISyntaxException e) {
			logger.error("Error occurred while building server URL");
			return Optional.empty();
		}
	}
}
