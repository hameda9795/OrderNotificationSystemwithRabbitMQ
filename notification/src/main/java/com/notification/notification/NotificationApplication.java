package com.notification.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Notification Service Application
 *
 * Provides order notification capabilities with asynchronous processing,
 * audit logging, and RabbitMQ message queue integration.
 *
 * Requirements:
 * - Database configuration (PostgreSQL/MySQL)
 * - RabbitMQ message broker
 * - Required configuration properties in application.yml/properties
 *
 * @author Spring Boot Development Team
 * @version 1.0
 * @since 2024
 */
@EnableAsync
@EnableJpaAuditing
@SpringBootApplication
public class NotificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationApplication.class, args);
	}

}
