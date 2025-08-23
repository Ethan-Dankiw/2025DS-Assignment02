package net.ethandankiw.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.data.HttpRequest;
import net.ethandankiw.data.RequestMethod;

public class RequestHandler {

	private RequestHandler() {
	}


	private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);


	public static void handleRequest(HttpRequest req) {
		// If the request has no method
		if (req.getMethod() == RequestMethod.NONE) {
			logger.warn("Unable to process request as there is no method");
		}

	}
}
