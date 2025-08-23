//package net.ethandankiw.network;
//
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.Optional;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import net.ethandankiw.utils.SocketUtils;
//
//public class HttpServer {
//
//	private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
//
//	private final String name;
//	private final Integer port;
//
//	public HttpServer(String name, Integer port) {
//		this.name = name;
//		this.port = port;
//	}
//
//
//	public void start() {
//		String serverName = name == null ? "SERVER" : name.toUpperCase();
//		logger.info("[{}] Starting on port {}...", serverName, port);
//
//		// Attempt to create a server socket on the provided port
//		Optional<ServerSocket> serverSocket = SocketUtils.createServerSocket(port);
//
//		// If the attempt was unsuccessful
//		if (serverSocket.isEmpty()) {
//			logger.error("[{}] Unable to start server on port {}", serverName, port);
//			return;
//		}
//
//		// If the attempt was successful, log the success
//		logger.info("[{}] Started on port {}", serverName, port);
//
//		// Store the socket on the http server
//		ServerSocket server = serverSocket.get();
//
//		// Then start listening for client requests
//		this.startListening(server);
//	}
//
//	private void startListening(ServerSocket server) {
//		// Attempt to spawn a new thread that will listen for incoming client connections
//		try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
//			// Spawn a thread that listens for incoming client connections
//			executor.submit(() -> {
//				// Get the name of the server
//				String serverName = name == null ? "SERVER" : name.toUpperCase();
//
//				// Have the thread constantly listen for incoming client connections
//				while (true) {
//					logger.info("[{}] Waiting for a client to connect...", serverName);
//
//					// Accept a connection from a client
//					this.acceptConnection(server);
//				}
//			});
//		} catch (Exception e) {
//			logger.warn("Unable to continue listening for client connections: {}", e.getMessage());
//		}
//	}
//
//	private void acceptConnection(ServerSocket server) {
//		// Accept a connection from a client
//		Optional<Socket> optionalConnection = SocketUtils.acceptClientConnection(server);
//
//		// If the connection was unable to be established
//		if (optionalConnection.isEmpty()) {
//			logger.error("Unable to accept a connection to the client");
//			return;
//		}
//
//		// Get the connection
//		Socket client = optionalConnection.get();
//
//		// Handle the client request
//		RequestHandler.
//	}
//}
