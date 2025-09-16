package net.ethandankiw;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.aggregation.LoadBalancer;
import net.ethandankiw.client.GetClient;
import net.ethandankiw.content.ContentServer;
import net.ethandankiw.data.http.HttpResponse;
import net.ethandankiw.data.store.FileManager;

class IntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);

	// Thread pool for handling client connections
	private static final ExecutorService getClientPool = Executors.newFixedThreadPool(10);

	// Thread pool for handling content server connections
	private static final ExecutorService contentServerPool = Executors.newFixedThreadPool(10);

	// Define URLs
	private static final String url = "http://localhost:4567";
	private static final String file = "IDS60901.txt";

	// Thread for handling the load balancer
	private static Thread loadBalancerThread;


	@BeforeEach
	void setup() throws InterruptedException {
		// Delete all cached content store files
		FileManager.deleteAllContentFiles();

		// Create a latch for starting up the load balancer
		CountDownLatch startupLatch = new CountDownLatch(1);

		// Start the LoadBalancer in a new thread
		loadBalancerThread = new Thread(() -> {
			try {
				String[] args = new String[]{String.valueOf(GlobalConstants.SERVER_PORT)};
				LoadBalancer.main(args);
				startupLatch.countDown();
			} catch (Exception ignored) {};
		});
		loadBalancerThread.start();

		// Wait for the LoadBalancer to start up
		startupLatch.await(5, TimeUnit.SECONDS);
	}

	@AfterEach
	void teardown() throws InterruptedException {
		// Stop the load balancer thread
		LoadBalancer.shutdown();

		loadBalancerThread.join(2000);

		// Reset the load balancer state
		LoadBalancer.reset();
		Thread.sleep(5000);

		// Delete all cached content store files
		FileManager.deleteAllContentFiles();
	}


	@Test
	void testNoServer() throws InterruptedException {
		logger.info("\n\n\n\n\nStarting testNoServer\n\n");

		teardown();
		CountDownLatch latch = new CountDownLatch(1);

		// Start the Content Server
		String[] args = new String[] { url, file };
		contentServerPool.submit(() -> {
			try {
				ContentServer.main(args);
			} finally {
				latch.countDown();
			}
		});

		// Wait for a reasonable amount of time for both tasks to complete
		boolean allTasksCompleted = latch.await(10, TimeUnit.SECONDS);

		// Assert that all tasks completed successfully
		Assertions.assertTrue(allTasksCompleted, "Tasks did not complete within the timeout period.");

		// Get the response from the GET client
		HttpResponse putResponse = ContentServer.storedResponse;

		// Check that the response exists
		Assertions.assertNull(putResponse,
				"Response from PUT client " + "should be null");

		logger.info("\n\n\ntestNoServer Done\n\n\n\n\n");
	}


	@Test
	void testPutAndGetRequest() throws InterruptedException {
		logger.info("\n\n\n\n\nStarting testPutAndGetRequest\n\n");
		CountDownLatch latch = new CountDownLatch(2);

		// Start the Content Server
		String[] args = new String[] { url, file };
		contentServerPool.submit(() -> {
			try {
				ContentServer.main(args);
			} finally {
				latch.countDown();
			}
		});

		// Add slight delay to GET client so that PUT arrives first
		// PUT requires file read which could be slow
		Thread.sleep(100);

		// Start the GET client to fetch the content
		getClientPool.submit(() -> {
			try {
				GetClient.main(args);
			} finally {
				latch.countDown();
			}
		});

		// Wait for a reasonable amount of time for both tasks to complete
		boolean allTasksCompleted = latch.await(10, TimeUnit.SECONDS);

		// Assert that all tasks completed successfully
		Assertions.assertTrue(allTasksCompleted, "Tasks did not complete within the timeout period.");

		// Get the response from the GET client
		HttpResponse putResponse = ContentServer.storedResponse;

		// Check that the response exists
		Assertions.assertNotNull(putResponse,
				"Response from PUT client " + "should not be null");

		// Check that the response code is valid
		Assertions.assertEquals(201, putResponse.getStatusCode(),
				"Response "
						+ "should indicate that a resource was created on the content"
						+ " store");

		// Get the response from the GET client
		HttpResponse getResponse = GetClient.storedResponse;

		// Check that the response exists
		Assertions.assertNotNull(getResponse, "Response from GET client should not be null");

		// Check that the response code is valid
		Assertions.assertEquals(200, getResponse.getStatusCode(),
				"Response "
						+ "should indicate that a resource was created on the content"
						+ " store");

		logger.info("\n\n\ntestPutAndGetRequest Done\n\n\n\n\n");
	}


	@Test
	void testInvalidGet() {
		logger.info("\n\n\n\n\nStarting testInvalidGet\n\n");
		// Start the Content Server
		String[] args = new String[] { url, file };

		// Start the GET client to fetch the content
		getClientPool.submit(() -> {
			GetClient.main(args);

			// Get the response from the GET client
			HttpResponse response = GetClient.storedResponse;

			// Check that the response exists
			Assertions.assertNotNull(response, "Response from GET client should not be null");

			// Check that the response code is valid
			Assertions.assertEquals(204, response.getStatusCode(), "Response "
					+ "should indicate that there is no weather data");

			logger.info("\n\n\ntestInvalidGet Done\n\n\n\n\n");
		});
	}

	@Test
	void testInvalidUrl() throws InterruptedException {
		logger.info("\n\n\n\n\nStarting testInvalidUrl\n\n");
		CountDownLatch latch = new CountDownLatch(1);

		// Run ContentServer with an invalid URL
		String[] args = new String[]{"http://bad-url", file};
		contentServerPool.submit(() -> {
			ContentServer.main(args);
			latch.countDown();
		});

		// Wait for the ContentServer to finish its attempts
		latch.await(10, TimeUnit.SECONDS);
		Assertions.assertNull(ContentServer.storedResponse, "Response should be null for invalid URL.");

		logger.info("\n\n\ntestInvalidUrl Done\n\n\n\n\n");
	}

	@Test
	void testDataPersistence() throws InterruptedException {
		logger.info("\n\n\n\n\nStarting testDataPersistence\n\n");
		CountDownLatch putLatch = new CountDownLatch(1);
		CountDownLatch getLatch = new CountDownLatch(1);

		// Step 1: Make a PUT request to save data
		String[] putArgs = new String[]{url, file};
		contentServerPool.submit(() -> {
			ContentServer.main(putArgs);
			putLatch.countDown();
		});
		putLatch.await(10, TimeUnit.SECONDS);

		// Step 2: Shut down the server to simulate a restart
		LoadBalancer.shutdown();
		loadBalancerThread.join(5000);

		// This is a short, but necessary, delay for ports to be released
		Thread.sleep(5000);
		LoadBalancer.reset();

		// Step 3: Restart the server
		CountDownLatch restartLatch = new CountDownLatch(1);
		loadBalancerThread = new Thread(() -> {
			try {
				String[] args = new String[]{String.valueOf(GlobalConstants.SERVER_PORT)};
				LoadBalancer.main(args);
			} catch (Exception ignored) {
				restartLatch.countDown();
			}
		});
		loadBalancerThread.start();
		restartLatch.await(5, TimeUnit.SECONDS);

		// Step 4: Make a GET request to verify the data is still there
		String[] getArgs = new String[]{url, file};
		getClientPool.submit(() -> {
			GetClient.main(getArgs);
			getLatch.countDown();
		});
		getLatch.await(10, TimeUnit.SECONDS);

		// Assert on the stored response from the GET client
		HttpResponse getResponse = GetClient.storedResponse;
		Assertions.assertNotNull(getResponse, "Response from GET client should not be null.");
		Assertions.assertEquals(200, getResponse.getStatusCode(), "GET should return 200 OK after restart.");

		logger.info("\n\n\ntestDataPersistence Done\n\n\n\n\n");
	}

	@Test
	void testMultipleConcurrentRequests() throws InterruptedException {
		logger.info("\n\n\n\n\nStarting testMultipleConcurrentRequests\n\n");
		int numRequests = 5;
		CountDownLatch putLatch = new CountDownLatch(numRequests);
		CountDownLatch getLatch = new CountDownLatch(numRequests);

		// Clear previous test data
		if (ContentServer.storedResponses != null) {
			ContentServer.storedResponses.clear();
		}
		if (ContentServer.storedResponses != null) {
			GetClient.storedResponses.clear();
		}

		// Step 1: Simulate concurrent PUT requests
		String[] putArgs = new String[]{url, file};
		for (int i = 0; i < numRequests; i++) {
			contentServerPool.submit(() -> {
				try {
					ContentServer.main(putArgs);
				} finally {
					putLatch.countDown();
				}
			});
		}

		// Wait for all PUT requests to complete
		Assertions.assertTrue(putLatch.await(10, TimeUnit.SECONDS), "PUT requests did not complete within timeout.");

		// Assert on the stored responses from the ContentServer
		long createdCount = ContentServer.storedResponses.stream().filter(r -> r.getStatusCode() == 201).count();
		long okCount = ContentServer.storedResponses.stream().filter(r -> r.getStatusCode() == 200).count();

		// There should be 5 content server responses
		Assertions.assertEquals(5, ContentServer.storedResponses.size(),
				"There should be 5 responses");

		// The first PUT should be 201, subsequent PUTs should be 200
		Assertions.assertEquals(1, createdCount, "There should be exactly one 201 CREATED response.");
		Assertions.assertEquals(numRequests - 1, okCount, "There should be " + (numRequests - 1) + " 200 OK responses.");

		// Step 2: Simulate concurrent GET requests
		String[] getArgs = new String[]{url, file};
		for (int i = 0; i < numRequests; i++) {
			getClientPool.submit(() -> {
				try {
					GetClient.main(getArgs);
				} finally {
					getLatch.countDown();
				}
			});
		}

		// Wait for all GET requests to complete
		getLatch.await(20, TimeUnit.SECONDS);

		// Assert on the stored responses from the GetClient
		long getOkCount = GetClient.storedResponses.stream().filter(r -> r.getStatusCode() == 200).count();
		Assertions.assertEquals(numRequests, getOkCount, "There should be " + numRequests + " 200 OK responses from GET client.");

		logger.info("\n\n\ntestMultipleConcurrentRequests Done\n\n\n\n\n");
	}
}
