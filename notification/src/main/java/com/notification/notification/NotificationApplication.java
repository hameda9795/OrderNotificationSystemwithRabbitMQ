package com.notification.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot Application for the Order Notification System.
 * Enables async processing, JPA auditing, and retry mechanisms.
 */
@EnableAsync
@EnableRetry
@EnableJpaAuditing
@SpringBootApplication
public class NotificationApplication {

	private static final Logger logger = LoggerFactory.getLogger(NotificationApplication.class);

	public static void main(String[] args) {
		try {
			logger.info("Starting Order Notification Application...");
			SpringApplication.run(NotificationApplication.class, args);
			logger.info("Order Notification Application started successfully");
		} catch (Exception e) {
			logger.error("Failed to start Order Notification Application", e);
			System.exit(1);
		}
	}
}

