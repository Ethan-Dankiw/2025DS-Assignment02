package net.ethandankiw.server;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.data.HttpServer;

public class ContentServer {

	public static void main(String[] args) {

		// If a different port number is provided
		if (args.length == 1) {
			// Get the argument from the command line
			String arg = args[0];

			// If the argument is valid
			if (arg != null && !arg.isBlank()) {
				// Parse the argument from string to int
				aggregationServerPort = Integer.parseInt(args[0]);
			}
		}

		// Create a new HTTP server
		HttpServer server = new HttpServer(ContentServer.class.getSimpleName(), aggregationServerPort);

		// Initialise the server
		server.init();

		// Create a load balancer for the HTTP server
		LoadBalancer balancer = new LoadBalancer(server);

		// Start balancing incoming client requests to the server
		balancer.startListening();
	}
}
