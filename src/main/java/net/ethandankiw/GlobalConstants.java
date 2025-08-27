package net.ethandankiw;

public enum GlobalConstants {
	/*
	 * Create a singleton instance of the global constants enum
	 */
	INSTANCE;

	// Port for client-server communication
	public static final Integer SERVER_PORT = 4567;

	// IP address for client-server communication
	public static final String SERVER_IP = "localhost";

	// Maximum number of threads to handle incoming client requests
	public static final Integer MAX_THREADS_FOR_CLIENT_REQUESTS = 100;

	// Default number of aggregation servers the load balancer starts with
	public static final Integer DEFAULT_BALANCED_SERVERS = 3;

	// Maximum allowed servers when balancing
	public static final Integer MAX_SERVERS = 10;

	// Minimum allowed server when balancing
	public static final Integer MIN_SERVERS = 1;
}
