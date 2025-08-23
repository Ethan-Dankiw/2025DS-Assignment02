package net.ethandankiw.network;

import java.net.ServerSocket;
import java.util.MissingResourceException;
import java.util.Optional;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.utils.ServerUtils;
import net.ethandankiw.utils.SocketUtils;

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

		// Create a server on a given port
		ServerSocket server = ServerUtils.createServer(aggregationServerPort);

		// Create a load balancer for the server
		RequestBalancer balancer = new RequestBalancer(server);

		// Start accepting connections to the server
		balancer.startAcceptingConnections();
	}
}
