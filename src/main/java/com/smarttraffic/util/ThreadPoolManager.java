package com.smarttraffic.util;

import java.util.concurrent.*;

/**
 * Thread Pool Manager demonstrating multithreading in Advanced Java.
 * Manages background tasks, async database operations, and periodic refresh.
 */
public class ThreadPoolManager {
    private static ThreadPoolManager instance;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;

    private ThreadPoolManager() {
        // Pool of daemon threads so they automatically release on application exit
        this.executorService = Executors.newFixedThreadPool(4, new ThreadFactory() {
            private int counter = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "SmartTraffic-Worker-" + (counter++));
                t.setDaemon(true);
                return t;
            }
        });

        this.scheduledExecutor = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private int counter = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "SmartTraffic-Scheduler-" + (counter++));
                t.setDaemon(true);
                return t;
            }
        });
    }

    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    public void execute(Runnable task) {
        executorService.execute(task);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public void shutdown() {
        try {
            executorService.shutdown();
            scheduledExecutor.shutdown();
        } catch (Exception ignored) {
        }
    }
}
