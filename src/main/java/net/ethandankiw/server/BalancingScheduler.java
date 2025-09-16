package net.ethandankiw.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BalancingScheduler {

	// Get the logger for this class
	private static final Logger logger = LoggerFactory.getLogger(BalancingScheduler.class);
	// What time unit should the scheduler use
	private static final TimeUnit timeUnit = TimeUnit.SECONDS;
	// Initial delay when starting the scheduler
	private static final Integer DEFAULT_INITIAL_DELAY = 5;      // seconds
	// How often should the scheduler balancer the servers
	private static final Integer DEFAULT_DELAY_PERIOD = 30;      // seconds
	// Separate thread for managing server scaling
	private static ScheduledExecutorService scheduler;
	// Scheduled task for server scaling
	private static ScheduledFuture<?> balancingTask;
	// Server being balanced
	private static ServerBalancerImpl balancer;
	private static Integer initialDelay = DEFAULT_INITIAL_DELAY; // seconds
	private static Integer delayPeriod = DEFAULT_DELAY_PERIOD;   // seconds


	private BalancingScheduler() {
	}


	/**
	 * Starts the load balancing scheduler. This method should be called only
	 * once.
	 */
	public static void startBalancingScheduler() {
		// Initialize the scheduler once
		if (scheduler == null || scheduler.isShutdown()) {
			scheduler = Executors.newSingleThreadScheduledExecutor();
			logger.info("Load Balancer scheduler initialized.");
		}

		// Schedule the task and keep a reference to the ScheduledFuture
		balancingTask = scheduler.scheduleAtFixedRate(balancer::balanceServers, initialDelay, delayPeriod, timeUnit);
		logger.info("Load Balancer started with an initial delay of {} seconds and will run every {} seconds", initialDelay, delayPeriod);

		// Cold start should be quick to balance, subsequent restarts should wait
		initialDelay = delayPeriod;
	}


	/**
	 * Restarts the load balancing scheduler with the current delay period. This
	 * function should be used for subsequent changes to the schedule.
	 */
	public static void restartBalancingScheduler() {
		if (scheduler == null || scheduler.isShutdown()) {
			logger.error("Scheduler is not running. Call startBalancingScheduler() first.");
			return;
		}

		if (balancingTask != null && !balancingTask.isDone()) {
			balancingTask.cancel(false);
			logger.info("Existing load balancer task cancelled. Rescheduling...");
		}

		// Subsequent restarts should use the full delay period to avoid rapid scheduling
		initialDelay = delayPeriod;

		balancingTask = scheduler.scheduleAtFixedRate(balancer::balanceServers, initialDelay, delayPeriod, timeUnit);
		logger.info("Load Balancer successfully restarted. Now running every {}s", delayPeriod);
	}


	public static void setBalancer(@NotNull ServerBalancerImpl balancer) {
		BalancingScheduler.balancer = balancer;
	}


	public static @NotNull Integer getDelayPeriod() {
		return delayPeriod;
	}


	public static void setDelayPeriod(@NotNull Integer period) {
		if (!period.equals(delayPeriod)) {
			logger.info("Load Balancer period changed from {}s to {}s", delayPeriod, period);
			delayPeriod = period;
			restartBalancingScheduler();
		}
	}


	public static void resetDelayPeriod() {
		setDelayPeriod(DEFAULT_DELAY_PERIOD);
	}


	public static void shutdown() {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
			logger.info("Load Balancer scheduler shut down.");
		}
	}

	public static void reset() {
		scheduler = null;
	}
}
