package net.ethandankiw.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestServer {

	private static final Logger logger = LoggerFactory.getLogger(RestServer.class);

	private final String name;
	private final Integer port;


	public RestServer(String name, Integer port) {
		this.name = name;
		this.port = port;
	}


	public String getName() {
		return name;
	}


	public Integer getPort() {
		return port;
	}
}
