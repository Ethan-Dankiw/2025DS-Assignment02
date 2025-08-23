package net.ethandankiw.data;

import java.util.Map;

public class HttpRequest {

	private RequestMethod method;
	private String path;

	private Map<String, String> headers;
	private String body;


	public HttpRequest() {
		method = RequestMethod.NONE;
		path = null;
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
}
