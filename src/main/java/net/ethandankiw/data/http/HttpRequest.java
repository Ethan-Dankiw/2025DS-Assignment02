package net.ethandankiw.data.http;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import net.ethandankiw.GlobalConstants;

public class HttpRequest {

	private HttpRequestMethod method;
	private String path;
	private String version;

	private Map<String, String> headers;
	private String body;


	public HttpRequest() {
		method = HttpRequestMethod.NONE;
		path = "/";
		version = GlobalConstants.HTTP_VERSION;
		headers = new HashMap<>();
		body = "";
	}


	public void parseAndSetMethod(String methodStr) {
		try {
			method = HttpRequestMethod.valueOf(methodStr.toUpperCase());
		} catch (IllegalArgumentException iae) {
			method = HttpRequestMethod.NONE;
		}
	}


	public HttpRequestMethod getMethod() {
		return method;
	}


	public void setMethod(HttpRequestMethod method) {
		this.method = method;
	}


	public String getPath() {
		return path;
	}


	public void setPath(String path) {
		this.path = path;
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
		// Request-Line: e.g., "PUT /weather.json HTTP/1.1"
		StringBuilder requestBuilder = new StringBuilder();
		requestBuilder.append(String.format("%s %s %s\r", getMethod().toString(), getPath(), getVersion()));
		requestBuilder.append("\n");

		// Add headers from the map
		String headerStr = getHeaders().entrySet()
									   .stream()
									   .map(set -> String.format("%s: %s", set.getKey(), set.getValue()))
									   .collect(Collectors.joining("\r\n"));
		requestBuilder.append(headerStr);
		requestBuilder.append("\r\n"); // Blank line to separate headers from body

		// Add the message body
		if (getBody() != null && !getBody().isEmpty()) {
			requestBuilder.append("\r\n"); // Blank line to separate headers from body
			requestBuilder.append(getBody());
		}

		return requestBuilder.toString();
	}
}
