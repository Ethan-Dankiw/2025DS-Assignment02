package net.ethandankiw.aggregation;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.network.HttpServer;

public class AggregationServer {

	public static void main(String[] args) {

		// Port of the Aggregation server
		// Default: 4567
		Integer aggregationServerPort = GlobalConstants.SERVER_PORT;

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
		HttpServer server = new HttpServer(AggregationServer.class.getName(), aggregationServerPort);

		// Start the server
		server.start();
	}
}
