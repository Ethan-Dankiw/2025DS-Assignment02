package net.ethandankiw.data.server;

import java.util.concurrent.PriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.GlobalConstants;
import net.ethandankiw.aggregation.AggregationServer;

public class ServerPoolImpl implements ServerPool {

	// Get the logger for this class
	private static final Logger logger = LoggerFactory.getLogger(ServerPoolImpl.class);

	// Create a priority queue where the lowest server load is first
	private final PriorityBlockingQueue<AggregationServer> servers;


	// Public constructor
	public ServerPoolImpl(int initialCapacity) {
		servers = new PriorityBlockingQueue<>(initialCapacity, new ServerLoadComparator());
	}


	/**
	 * Returns all servers currently registered in the pool.
	 * <p>
	 * The returned {@link Iterable} is intended for read-only iteration.
	 *
	 * @return an {@code Iterable} of all servers in the pool
	 */
	@Override
	public Iterable<AggregationServer> getAllServers() {
		return servers;
	}


	/**
	 * Registers an existing aggregation server in the pool.
	 *
	 * @param server the server instance to register
	 */
	@Override
	public void register(AggregationServer server) {
		// If the server does not exist
		if (server == null) {
			logger.info("Cannot add server to priority queue as the server is null");
			return;
		}

		// Put the server back in the list of servers
		boolean success = servers.offer(server);

		// If the server was not put in the list
		if (!success) {
			logger.error("Failed to re-add server to the priority queue: {}", server);
		}
	}


	/**
	 * Creates a new aggregation server, registers it in the pool, and returns the newly created instance.
	 *
	 * @return the newly created and registered server
	 */
	@Override
	public AggregationServer createAndRegister() {
		// Create a new server
		AggregationServer server = new AggregationServer();

		// Add the server to the queue
		register(server);
		logger.info("Added new server {}, current active count = {}", server.getUUID(), servers.size());

		// Return the newly created server
		return server;
	}


	/**
	 * Returns a server that is available to handle new requests.
	 * <p>
	 * By default, this is the server with the lowest current request load. If all existing servers are at capacity, a
	 * new server will be created and returned.
	 *
	 * @return the least-loaded available server, or a newly created server if none are available
	 */
	@Override
	public AggregationServer getAvailableServer() {
		// Get the least loaded server
		AggregationServer server = getLeastLoadedServer();

		// If the server does not exist
		if (server == null) {
			logger.debug("Creating new server as no servers currently exist");

			// Create and add a new server to the list
			return createAndRegister();
		}

		// If the server does exist
		// Check if the server is at capacity
		if (server.atCapacity()) {
			logger.debug("Creating new server as least loaded server is at capacity");

			// Create and add a new server to the list
			return createAndRegister();
		}

		// Return the least loaded server that is not at capacity
		return server;
	}


	/**
	 * Returns the server with the lowest current request load.
	 * <p>
	 * Does not remove the server from the pool.
	 *
	 * @return the least-loaded server, or {@code null} if none are available
	 */
	@Override
	public AggregationServer getLeastLoadedServer() {
		// Get the server with the least load
		AggregationServer server = popLeastLoadedServer();

		// Put the server back into the list of servers
		register(server);

		// Return the least loaded server
		return server;
	}


	/**
	 * Returns the server with the lowest current request load without removing it from the pool.
	 *
	 * @return the least-loaded server, or {@code null} if none are available
	 */
	@Override
	public AggregationServer peekLeastLoadedServer() {
		return servers.peek();
	}


	/**
	 * Removes and returns the server with the lowest current request load from the pool.
	 *
	 * @return the removed server, or {@code null} if none are available
	 */
	@Override
	public AggregationServer popLeastLoadedServer() {
		return servers.poll();
	}


	/**
	 * Calculates the average request load across all servers in the pool.
	 *
	 * @return the average load, or {@code 0.0} if no servers are registered
	 */
	@Override
	public Double calculateAverageServerLoad() {
		// Calculate the average
		return servers.stream()
					  .mapToDouble(AggregationServer::getLoad)
					  .average()
					  .orElse(0.0);
	}


	/**
	 * Returns the total number of servers currently registered in the pool.
	 *
	 * @return the number of registered servers
	 */
	@Override
	public Integer getServerCount() {
		return servers.size();
	}


	/**
	 * Prints the server load stats for each server.
	 */
	@Override
	public void printStats() {
		logger.info("Stats for the current servers");

		// Log the breakdown of server load for each server
		for (AggregationServer server : servers) {
			logger.info("Server {} has {} active requests out of {}", server.getUUID(), server.getActiveRequestsCount(), GlobalConstants.MAX_THREADS_FOR_CLIENT_REQUESTS);
		}
	}
}
