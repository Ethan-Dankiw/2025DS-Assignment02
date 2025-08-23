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
}
