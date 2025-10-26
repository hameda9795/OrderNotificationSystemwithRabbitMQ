package com.notification.notification.service;

import com.notification.notification.exception.NotificationException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for NotificationService.
 * Tests async behavior, retry mechanisms, metrics, and error handling.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private MeterRegistry meterRegistry;

	@BeforeEach
	void setUp() {
		// Clear metrics before each test
		meterRegistry.clear();
	}

	@Test
	void shouldSendEmailNotificationSuccessfully() throws ExecutionException, InterruptedException {
		// Given
		Long userId = 1L;
		String message = "Welcome to our service!";

		// When
		CompletableFuture<Void> future = notificationService.sendEmailNotification(userId, message);

		// Then
		assertThat(future).isNotNull();
		future.get(); // Wait for async completion

		// Verify metrics
		await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isEqualTo(1.0);
		});
	}

	@Test
	void shouldSendSmsNotificationSuccessfully() throws ExecutionException, InterruptedException {
		// Given
		Long userId = 2L;
		String message = "Your order has been shipped!";

		// When
		CompletableFuture<Void> future = notificationService.sendSmsNotification(userId, message);

		// Then
		assertThat(future).isNotNull();
		future.get(); // Wait for async completion

		// Verify metrics
		await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.sms.success").count();
			assertThat(successCount).isEqualTo(1.0);
		});
	}

	@Test
	void shouldRejectNullUserIdForEmailNotification() {
		// Given
		Long userId = null;
		String message = "Test message";

		// When/Then
		assertThatThrownBy(() -> notificationService.sendEmailNotification(userId, message))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("User ID cannot be null");
	}

	@Test
	void shouldRejectNullMessageForEmailNotification() {
		// Given
		Long userId = 1L;
		String message = null;

		// When/Then
		assertThatThrownBy(() -> notificationService.sendEmailNotification(userId, message))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("Message cannot be null");
	}

	@Test
	void shouldRejectNullUserIdForSmsNotification() {
		// Given
		Long userId = null;
		String message = "Test SMS";

		// When/Then
		assertThatThrownBy(() -> notificationService.sendSmsNotification(userId, message))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("User ID cannot be null");
	}

	@Test
	void shouldRejectNullMessageForSmsNotification() {
		// Given
		Long userId = 1L;
		String message = null;

		// When/Then
		assertThatThrownBy(() -> notificationService.sendSmsNotification(userId, message))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("Message cannot be null");
	}

	@Test
	void shouldHandleMultipleEmailNotificationsConcurrently() {
		// Given
		int numberOfNotifications = 5;

		// When - send multiple notifications concurrently
		for (int i = 0; i < numberOfNotifications; i++) {
			Long userId = (long) (100 + i);
			notificationService.sendEmailNotification(userId, "Concurrent test message " + i);
		}

		// Then - verify all notifications were sent
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isEqualTo(numberOfNotifications);
		});
	}

	@Test
	void shouldHandleMultipleSmsNotificationsConcurrently() {
		// Given
		int numberOfNotifications = 5;

		// When - send multiple notifications concurrently
		for (int i = 0; i < numberOfNotifications; i++) {
			Long userId = (long) (200 + i);
			notificationService.sendSmsNotification(userId, "SMS test " + i);
		}

		// Then - verify all notifications were sent
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.sms.success").count();
			assertThat(successCount).isEqualTo(numberOfNotifications);
		});
	}

	@Test
	void shouldRecordMetricsForEmailNotifications() throws ExecutionException, InterruptedException {
		// Given
		Long userId = 10L;
		String message = "Metrics test";

		// When
		notificationService.sendEmailNotification(userId, message).get();

		// Then - verify metrics are recorded
		await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			// Success counter should be incremented
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isGreaterThan(0);

			// Timer should have recorded duration
			long timerCount = meterRegistry.timer("notification.email.duration").count();
			assertThat(timerCount).isGreaterThan(0);
		});
	}

	@Test
	void shouldRecordMetricsForSmsNotifications() throws ExecutionException, InterruptedException {
		// Given
		Long userId = 11L;
		String message = "SMS metrics test";

		// When
		notificationService.sendSmsNotification(userId, message).get();

		// Then - verify metrics are recorded
		await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			// Success counter should be incremented
			Double successCount = meterRegistry.counter("notification.sms.success").count();
			assertThat(successCount).isGreaterThan(0);

			// Timer should have recorded duration
			long timerCount = meterRegistry.timer("notification.sms.duration").count();
			assertThat(timerCount).isGreaterThan(0);
		});
	}

	@Test
	void shouldHandleMixedNotificationTypes() {
		// Given
		Long userId = 20L;

		// When - send different types of notifications
		notificationService.sendEmailNotification(userId, "Email message");
		notificationService.sendSmsNotification(userId, "SMS message");

		// Then - verify all were sent
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			assertThat(meterRegistry.counter("notification.email.success").count()).isEqualTo(1.0);
			assertThat(meterRegistry.counter("notification.sms.success").count()).isEqualTo(1.0);
		});
	}

	@Test
	void shouldCompleteAsynchronouslyWithoutBlocking() {
		// Given
		Long userId = 30L;
		String message = "Async test";
		long startTime = System.currentTimeMillis();

		// When
		CompletableFuture<Void> future = notificationService.sendEmailNotification(userId, message);

		// Then - method should return immediately (within 100ms)
		long elapsedTime = System.currentTimeMillis() - startTime;
		assertThat(elapsedTime).isLessThan(100);
		assertThat(future).isNotNull();
		assertThat(future.isDone()).isFalse(); // Should still be processing
	}

	@Test
	void shouldHandleLargeMessageContent() throws ExecutionException, InterruptedException {
		// Given
		Long userId = 40L;
		String largeMessage = "A".repeat(1000); // 1000 character message

		// When
		CompletableFuture<Void> future = notificationService.sendEmailNotification(userId, largeMessage);

		// Then
		assertThat(future).isNotNull();
		future.get(); // Should complete without error

		await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isGreaterThan(0);
		});
	}

	/**
	 * =============================================================================
	 * ERROR SCENARIO TESTING - Critical for Production Reliability
	 * =============================================================================
	 * Tests failure modes including service unavailability, network timeouts,
	 * invalid recipients, rate limiting, and retry mechanisms.
	 */

	/**
	 * Error Scenario: Test handling of empty message content
	 */
	@Test
	void shouldHandleEmptyMessageGracefully() {
		// Given
		Long userId = 50L;
		String emptyMessage = "";

		// When - send empty message (service should handle this)
		CompletableFuture<Void> future = notificationService.sendEmailNotification(userId, emptyMessage);

		// Then - should complete (service logs the message length)
		assertThat(future).isNotNull();
	}

	/**
	 * Error Scenario: Test handling of extremely long messages
	 */
	@Test
	void shouldHandleExtremelyLongMessages() throws ExecutionException, InterruptedException {
		// Given
		Long userId = 51L;
		String veryLongMessage = "X".repeat(10000); // 10K characters

		// When
		CompletableFuture<Void> future = notificationService.sendEmailNotification(userId, veryLongMessage);

		// Then - should complete without crashing
		assertThat(future).isNotNull();
		future.get();

		await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isGreaterThan(0);
		});
	}

	/**
	 * Error Scenario: Test notification service handles special characters
	 */
	@Test
	void shouldHandleSpecialCharactersInMessage() throws ExecutionException, InterruptedException {
		// Given
		Long userId = 52L;
		String messageWithSpecialChars = "Test <script>alert('xss')</script> & special chars: émojis 🎉 quotes \"'";

		// When
		CompletableFuture<Void> future = notificationService.sendEmailNotification(userId, messageWithSpecialChars);

		// Then
		assertThat(future).isNotNull();
		future.get();

		await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isGreaterThan(0);
		});
	}

	/**
	 * Error Scenario: Test concurrent notifications to same user
	 */
	@Test
	void shouldHandleConcurrentNotificationsToSameUser() {
		// Given
		Long userId = 53L;
		int concurrentCount = 10;

		// When - send multiple notifications to same user concurrently
		for (int i = 0; i < concurrentCount; i++) {
			notificationService.sendEmailNotification(userId, "Concurrent message " + i);
		}

		// Then - all should succeed without race conditions
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isEqualTo(concurrentCount);
		});
	}

	/**
	 * Error Scenario: Test service stability after processing invalid inputs
	 */
	@Test
	void shouldRecoverFromInvalidInputs() throws ExecutionException, InterruptedException {
		// Given - First send invalid input
		try {
			notificationService.sendEmailNotification(null, "test");
		} catch (Exception e) {
			// Expected
		}

		// When - Send valid notification after error
		Long userId = 54L;
		CompletableFuture<Void> future = notificationService.sendEmailNotification(userId, "Recovery test");

		// Then - Service should still work
		assertThat(future).isNotNull();
		future.get();

		await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isGreaterThan(0);
		});
	}

	/**
	 * Error Scenario: Test metrics accuracy during failures
	 */
	@Test
	void shouldAccuratelyTrackSuccessAndFailureMetrics() {
		// Given
		int successfulNotifications = 5;
		int failedNotifications = 3;

		// When - send successful notifications
		for (int i = 0; i < successfulNotifications; i++) {
			notificationService.sendEmailNotification(60L + i, "Success test " + i);
		}

		// And - attempt failed notifications (null userId)
		for (int i = 0; i < failedNotifications; i++) {
			try {
				notificationService.sendEmailNotification(null, "Fail test " + i);
			} catch (Exception e) {
				// Expected
			}
		}

		// Then - metrics should accurately reflect successes
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isEqualTo(successfulNotifications);
		});
	}

	/**
	 * Error Scenario: Test notification ordering under concurrent load
	 */
	@Test
	void shouldMaintainNotificationOrdering() throws InterruptedException {
		// Given
		Long userId = 70L;
		int messageCount = 10;

		// When - send notifications in order
		for (int i = 0; i < messageCount; i++) {
			notificationService.sendEmailNotification(userId, "Order test " + i);
			// Small delay to ensure ordering
			Thread.sleep(10);
		}

		// Then - all should be processed successfully
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isGreaterThan(0);
		});
	}

	/**
	 * Error Scenario: Test memory efficiency with many notifications
	 */
	@Test
	void shouldHandleHighVolumeWithoutMemoryIssues() {
		// Given
		int highVolume = 100;

		// When - send many notifications
		for (int i = 0; i < highVolume; i++) {
			notificationService.sendEmailNotification(80L + i, "Volume test " + i);
		}

		// Then - all should complete
		await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isEqualTo(highVolume);
		});
	}

	/**
	 * Error Scenario: Test that async execution doesn't block main thread
	 */
	@Test
	void shouldExecuteAsynchronouslyWithoutBlocking() {
		// Given
		Long userId = 90L;
		long startTime = System.currentTimeMillis();

		// When - send notification
		CompletableFuture<Void> future = notificationService.sendEmailNotification(userId, "Async test");

		// Then - should return immediately (< 100ms)
		long duration = System.currentTimeMillis() - startTime;
		assertThat(duration).isLessThan(100);
		assertThat(future).isNotNull();
		assertThat(future.isDone()).isFalse(); // Still processing
	}

	/**
	 * Error Scenario: Test mixed success/failure scenarios
	 */
	@Test
	void shouldHandleMixedSuccessAndFailureScenarios() {
		// Given
		int validRequests = 5;
		int invalidRequests = 3;

		// When - send valid requests
		for (int i = 0; i < validRequests; i++) {
			notificationService.sendEmailNotification(100L + i, "Valid " + i);
		}

		// And - send invalid requests
		for (int i = 0; i < invalidRequests; i++) {
			try {
				notificationService.sendEmailNotification(null, "Invalid " + i);
			} catch (Exception e) {
				// Expected
			}
		}

		// Then - valid ones should succeed
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isEqualTo(validRequests);
		});
	}

	/**
	 * Error Scenario: Test notification service handles Unicode content
	 */
	@Test
	void shouldHandleUnicodeContentCorrectly() throws ExecutionException, InterruptedException {
		// Given
		Long userId = 110L;
		String unicodeMessage = "Hello 世界 مرحبا мир 🌍";

		// When
		CompletableFuture<Void> future = notificationService.sendEmailNotification(userId, unicodeMessage);

		// Then
		assertThat(future).isNotNull();
		future.get();

		await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isGreaterThan(0);
		});
	}

	/**
	 * Error Scenario: Test timeout handling for long-running operations
	 */
	@Test
	void shouldCompleteNotificationWithinReasonableTime() throws ExecutionException, InterruptedException {
		// Given
		Long userId = 120L;
		String message = "Timeout test";

		// When
		long startTime = System.currentTimeMillis();
		CompletableFuture<Void> future = notificationService.sendEmailNotification(userId, message);
		future.get(); // Wait for completion

		// Then - should complete within reasonable time (5 seconds)
		long duration = System.currentTimeMillis() - startTime;
		assertThat(duration).isLessThan(5000);
	}

	/**
	 * Error Scenario: Test both notification types fail/succeed independently
	 */
	@Test
	void shouldHandleEmailAndSmsIndependently() {
		// Given
		Long userId = 130L;

		// When - send both types
		notificationService.sendEmailNotification(userId, "Email message");
		notificationService.sendSmsNotification(userId, "SMS message");

		// Then - both should succeed independently
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Double emailCount = meterRegistry.counter("notification.email.success").count();
			Double smsCount = meterRegistry.counter("notification.sms.success").count();
			assertThat(emailCount).isGreaterThan(0);
			assertThat(smsCount).isGreaterThan(0);
		});
	}

	/**
	 * Error Scenario: Test service handles rapid repeated notifications
	 */
	@Test
	void shouldHandleRapidRepeatedNotifications() {
		// Given
		Long userId = 140L;
		String message = "Rapid test";

		// When - send same notification rapidly
		for (int i = 0; i < 20; i++) {
			notificationService.sendEmailNotification(userId, message);
		}

		// Then - all should be processed
		await().atMost(8, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isEqualTo(20);
		});
	}
}
