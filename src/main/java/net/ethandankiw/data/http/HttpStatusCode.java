package net.ethandankiw.data.http;

public enum HttpStatusCode {
	// Default status
	NONE(-1, "UNKNOWN STATUS CODE"),

	// Successful Responses
	OK(200, "OK"),
	CREATED(201, "CREATED"),
	NO_CONTENT(204, "NO CONTENT"),

	// Client Error Responses
	BAD_REQUEST(400, "BAD REQUEST"),
	UNAUTHORIZED(401, "UNAUTHORIZED"),
	NOT_FOUND(404, "NOT FOUND"),
	METHOD_NOT_ALLOWED(405, "METHOD NOT ALLOWED"),
	REQUEST_TIMEOUT(408, "REQUEST TIMEOUT"),
	GONE(410, "GONE"),
	TOO_MANY_REQUESTS(429, "TOO MANY REQUESTS"),

	// Server Error Responses
	INTERNAL_SERVER_ERROR(500, "INTERNAL SERVER ERROR"),
	HTTP_VERSION_NOT_SUPPORTED(505, "HTTP VERSION NOT SUPPORTED"),
	INSUFFICIENT_STORAGE(507, "INSUFFICIENT STORAGE");

	private final Integer statusCode;
	private final String reasonPhrase;


	HttpStatusCode(Integer statusCode, String reasonPhrase) {
		this.statusCode = statusCode;
		this.reasonPhrase = reasonPhrase;
	}


	public static HttpStatusCode valueOf(int statusCode) {
		for (HttpStatusCode code : values()) {
			if (code.getStatusCode() == statusCode) {
				return code;
			}
		}
		return NONE;
	}


	public Integer getStatusCode() {
		return this.statusCode;
	}


	public String getReasonPhrase() {
		return this.reasonPhrase;
	}


	public static boolean isRetryable(Integer statusCode) {
		// If the status code is an invalid server-side error
		// 505 == Invalid HTTP version
		if (statusCode == 505) {
			return false;
		}

		// Server-side errors are generally retryable
		return statusCode >= 500 && statusCode < 600;
	}

	public static boolean isSuccess(Integer statusCode) {
		// Does the status code indicate a success
		return statusCode >= 200 && statusCode < 300;
	}
}
