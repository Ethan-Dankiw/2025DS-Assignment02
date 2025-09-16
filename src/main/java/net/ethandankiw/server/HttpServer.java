package net.ethandankiw.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.utils.SocketUtils;

public class HttpServer {

	private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

	private final String name;
	private final Integer port;

	private ServerSocket socket;


	public HttpServer(@NotNull String name, @NotNull Integer port) {
		this.name = name;
		this.port = port;
	}


	public void start() {
		logger.info("[{}] Server starting...", getName());

		// Attempt to create a server socket on the provided port
		Optional<ServerSocket> serverSocket = SocketUtils.createServerSocket(getPort());

		// If the attempt was unsuccessful
		if (serverSocket.isEmpty()) {
			logger.error("[{}] Unable to start server on port {}", getName(), getPort());
			return;
		}

		// If the attempt was successful, log the success
		logger.info("[{}] Accepting requests on port {}", getName(), getPort());

		// Store the socket on the http server
		this.socket = serverSocket.get();
	}


	@NotNull
	public String getName() {
		return name;
	}


	@NotNull
	public Integer getPort() {
		return port;
	}


	@Nullable
	public ServerSocket getSocket() {
		return socket;
	}


	public void shutdown() throws IOException {
		this.socket.close();
	}
}
