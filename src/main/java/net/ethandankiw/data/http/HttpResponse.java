package net.ethandankiw.data.http;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import net.ethandankiw.GlobalConstants;

public class HttpResponse {

	private HttpStatusCode status;
	private String version;

	private Map<String, String> headers;
	private String body;


	public HttpResponse() {
		status = HttpStatusCode.NONE;
		version = GlobalConstants.HTTP_VERSION;
		headers = new HashMap<>();
		body = "";
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


	public void setVersion(String version) {
		this.version = version;
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
		String statusLine = String.format("%s %d %s", this.version, this.status.getStatusCode(), this.status.getReasonPhrase());

		// Header lines: e.g., "Content-Type: application/json"
		String headerStr = this.headers.entrySet()
									   .stream()
									   .map(set -> String.format("%s: %s", set.getKey(), set.getValue()))
									   .collect(Collectors.joining("\r\n")); // Use \r\n for line endings

		StringBuilder responseBuilder = new StringBuilder();
		responseBuilder.append(statusLine)
					   .append("\r\n");
		responseBuilder.append(headerStr)
					   .append("\r\n");
		responseBuilder.append("\r\n"); // Blank line to separate headers from body

		if (body != null) {
			responseBuilder.append(body);
		}

		return responseBuilder.toString();
	}
}
