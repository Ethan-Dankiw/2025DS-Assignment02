package net.ethandankiw.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;

public class AggregationServer {
	private static final Logger logger = LoggerFactory.getLogger(AggregationServer.class);

	public static void main(String[] args) {

		// Port of the Aggregation server
		// Default: 4567
		Integer AggregationPort = GlobalConstants.SERVER_PORT;

		// If a different port number is provided
		if (args.length == 1) {
			String arg = args[0];

			// If the argument is valid
			if (arg != null && !arg.isBlank()) {
				// Parse the argument from string to int
				AggregationPort = Integer.parseInt(args[0]);
			}
		}

		// Start up a server on the given port

		// Print all the args
		for (String arg : args) {
			logger.info("AggregationServer Arg: {}", arg);
		}
	}
}
