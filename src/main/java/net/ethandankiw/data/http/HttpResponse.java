package net.ethandankiw.data.http;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import net.ethandankiw.GlobalConstants;

public class HttpResponse {

	private final String version;
	private Map<String, String> headers;
	private HttpStatusCode status;
	private String body;


	public HttpResponse() {
		status = HttpStatusCode.NONE;
		version = GlobalConstants.HTTP_VERSION;
		headers = new HashMap<>();
		body = "";
	}


	public Integer getStatusCode() {
		return status.getStatusCode();
	}


	public HttpStatusCode getStatus() {
		return status;
	}


	public void setStatus(HttpStatusCode status) {
		this.status = status;
	}


	public String getVersion() {
		return version;
	}


	public Map<String, String> getHeaders() {
		return headers;
	}


	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}


	public void addHeader(String key, String value) {
		this.headers.put(key, value);
	}


	public String getBody() {
		return body;
	}


	public void setBody(String body) {
		this.body = body;
	}


	@Override
	public String toString() {
		// Status Line: e.g., "HTTP/1.1 200 OK"
		StringBuilder responseBuilder = new StringBuilder();
		responseBuilder.append(String.format("%s %d %s\r", getVersion(), getStatus().getStatusCode(), getStatus().getReasonPhrase()));
		responseBuilder.append("\n");

		// Header lines: e.g., "Content-Type: application/json"
		String headerStr = getHeaders().entrySet()
									   .stream()
									   .map(set -> String.format("%s: %s", set.getKey(), set.getValue()))
									   .collect(Collectors.joining("\r\n")); // Use \r\n for line endings
		responseBuilder.append(headerStr);
		responseBuilder.append("\r\n"); // Blank line to separate headers from body

		// Add the message body
		if (getBody() != null && !getBody().isEmpty()) {
			responseBuilder.append("\r\n"); // Blank line to separate headers from body
			responseBuilder.append(getBody());
		}

		return responseBuilder.toString();
	}
}
