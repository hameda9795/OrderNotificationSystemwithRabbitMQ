package com.notification.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.config.ConfigDataException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;


@EnableAsync
@EnableJpaAuditing
@SpringBootApplication
public class NotificationApplication {

	private static final Logger logger = LoggerFactory.getLogger(NotificationApplication.class);

	public static void main(String[] args) {
		try {
			logger.info("Starting Order Notification Application...");
			SpringApplication.run(NotificationApplication.class, args);
			logger.info("Order Notification Application started successfully");
		} catch (ConfigDataException e) {
			logger.error("Configuration error during startup", e);
			System.exit(2);
		} catch (DataAccessException e) {
			logger.error("Database connection failed during startup", e);
			System.exit(3);
		} catch (Exception e) {
			logger.error("Unexpected error during startup", e);
			System.exit(1);
		}
	}

}
