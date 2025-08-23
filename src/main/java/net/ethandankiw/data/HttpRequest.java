package net.ethandankiw.data;

import java.util.Map;
import java.util.stream.Collectors;

public class HttpRequest {

	private RequestMethod method;
	private String path;
	private String version;

	private Map<String, String> headers;
	private String body;


	public HttpRequest() {
		method = RequestMethod.NONE;
		path = null;
		version = null;
		headers = null;
		body = null;
	}


	public void parseAndSetMethod(String methodStr) {
		try {
			method = RequestMethod.valueOf(methodStr.toUpperCase());
		} catch (IllegalArgumentException iae) {
			method = RequestMethod.NONE;
		}
	}


	public RequestMethod getMethod() {
		return method;
	}


	public void setMethod(RequestMethod method) {
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


	public String getBody() {
		return body;
	}


	public void setBody(String body) {
		this.body = body;
	}


	@Override
	public String toString() {
		// Correctly format the request line
		String requestStr = String.format("%n%n%s %s %s%n%n", this.method, this.path, this.version);

		// Stream the header entries, format each one, and join them with a newline
		String headerStr = this.headers.entrySet().stream()
									 .map(set -> String.format("%s -> %s", set.getKey(), set.getValue()))
									 .collect(Collectors.joining("\n"));

		// Add a final newline for a clean break
		return requestStr + headerStr + "\n\n" + body + "\n";
	}
}
