package net.ethandankiw.server;

import net.ethandankiw.aggregation.AggregationServer;

public interface ServerPool {

	/**
	 * Returns all servers currently registered in the pool.
	 * <p>
	 * The returned {@link Iterable} is intended for read-only iteration.
	 *
	 * @return an {@code Iterable} of all servers in the pool
	 */
	Iterable<AggregationServer> getAllServers();

	/**
	 * Registers an existing aggregation server in the pool.
	 *
	 * @param server the server instance to register
	 */
	void register(AggregationServer server);

	/**
	 * Creates a new aggregation server, registers it in the pool, and returns the newly created instance.
	 *
	 * @return the newly created and registered server
	 */
	AggregationServer createAndRegister();

	/**
	 * Returns a server that is available to handle new requests.
	 * <p>
	 * By default, this is the server with the lowest current request load. If all existing servers are at capacity, a
	 * new server will be created and returned.
	 *
	 * @return the least-loaded available server, or a newly created server if none are available
	 */
	AggregationServer getAvailableServer();

	/**
	 * Returns the server with the lowest current request load.
	 * <p>
	 * Does not remove the server from the pool.
	 *
	 * @return the least-loaded server, or {@code null} if none are available
	 */
	AggregationServer getLeastLoadedServer();

	/**
	 * Returns the server with the lowest current request load without removing it from the pool.
	 *
	 * @return the least-loaded server, or {@code null} if none are available
	 */
	AggregationServer peekLeastLoadedServer();

	/**
	 * Removes and returns the server with the lowest current request load from the pool.
	 *
	 * @return the removed server, or {@code null} if none are available
	 */
	AggregationServer popLeastLoadedServer();

	/**
	 * Calculates the average request load across all servers in the pool.
	 *
	 * @return the average load, or {@code 0.0} if no servers are registered
	 */
	Double calculateAverageServerLoad();

	/**
	 * Returns the total number of servers currently registered in the pool.
	 *
	 * @return the number of registered servers
	 */
	Integer getServerCount();

	/**
	 * Prints the server load stats for each server.
	 */
	void printStats();
}
