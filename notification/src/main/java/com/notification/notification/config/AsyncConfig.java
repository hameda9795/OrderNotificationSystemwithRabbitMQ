package com.notification.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Asynchronous processing configuration with proper thread pool management.
 * Configures thread pools for async operations and error handling.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.java);

    /**
     * Configures the async task executor with appropriate thread pool settings.
     *
     * @return Configured ThreadPoolTaskExecutor
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - minimum number of threads to keep alive
        executor.setCorePoolSize(10);

        // Maximum pool size - maximum number of threads
        executor.setMaxPoolSize(50);

        // Queue capacity - number of tasks to queue before rejecting
        executor.setQueueCapacity(100);

        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("NotificationAsync-");

        // Rejection policy - caller runs the task when thread pool is full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Allow core threads to timeout
        executor.setAllowCoreThreadTimeOut(true);

        // Keep alive time in seconds
        executor.setKeepAliveSeconds(60);

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Await termination timeout in seconds
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        logger.info("Async task executor configured with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * Handles uncaught exceptions in async methods.
     *
     * @return AsyncUncaughtExceptionHandler that logs exceptions
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            logger.error("Uncaught exception in async method '{}': {}",
                    method.getName(), throwable.getMessage(), throwable);

            // TODO: Send to monitoring/alerting system
            // e.g., alertingService.sendAlert("Async execution failed", throwable);
        };
    }
}
