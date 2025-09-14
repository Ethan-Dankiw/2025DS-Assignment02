package net.ethandankiw.utils.http;

import java.net.Socket;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.http.HttpResponse;
import net.ethandankiw.data.http.HttpStatusCode;
import net.ethandankiw.utils.SocketUtils;

public class HttpResponseUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpResponseUtils.class);


	private HttpResponseUtils() {
	}


	public static void generateAndSendResponse(Socket client, HttpStatusCode status, @NotNull String body, long clockValue) {
		// Generate a response
		// Increment clock value as response counts as causal event
		HttpResponse response = HttpResponseUtils.generateResponse(status, body, clockValue + 1);

		// Send the complete response to the client
		boolean success = SocketUtils.writeToSocket(client, response.toString());

		// If the response cannot be written to the client
		if (!success) {
			logger.error("Unable to send response to client");
		}
	}


	public static HttpResponse generateResponse(HttpStatusCode status, @NotNull String body, long clockValue) {
		// If there is no status
		if (status == null || status.equals(HttpStatusCode.NONE)) {
			status = HttpStatusCode.BAD_REQUEST;
		}

		// Create a new response object
		HttpResponse response = new HttpResponse();

		// Set the status and the body
		response.setStatus(status);

		// Get the status code and reason phrase
		Integer code = status.getStatusCode();

		// Add the lamport clock to the response
		response.addHeader(GlobalConstants.LAMPORT_CLOCK_HEADER, String.valueOf(clockValue));

		// If the response is successful
		if (code >= 200 && code < 300) {
			logger.debug("Successful Response Detected: {} {}", code, status.getReasonPhrase());
			// A 204 No Content response should not have a body or content-related headers
			if (code == 204) {
				response.setBody("");
			} else {
				response.setBody(body);
				response.addHeader("Content-Type", "application/json");
				response.addHeader("Content-Length", String.valueOf(body.length()));
			}
		}
		// If the response is a client or server error
		else if (code >= 400 && code < 600) {
			logger.debug("Unsuccessful Response Detected: {} {}", code, status.getReasonPhrase());
			response.setBody(body);
			response.addHeader("Content-Type", "text/plain");
			response.addHeader("Content-Length", String.valueOf(body.length()));
		}

		// Return the response object
		return response;
	}
}
