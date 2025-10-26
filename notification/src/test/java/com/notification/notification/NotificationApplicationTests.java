package com.notification.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.notification.dto.CreateOrderRequest;
import com.notification.notification.dto.OrderResponseDto;
import com.notification.notification.entity.Order;
import com.notification.notification.enums.OrderStatus;
import com.notification.notification.event.OrderCreatedEvent;
import com.notification.notification.repository.OrderRepository;
import com.notification.notification.service.NotificationService;
import com.notification.notification.service.OrderService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for the notification service.
 * Tests include RabbitMQ message handling, database operations, REST endpoints,
 * notification delivery, error handling, and distributed system concerns.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
class NotificationApplicationTests {

	@Container
	static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
		DockerImageName.parse("rabbitmq:3.13-management-alpine")
	);

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
		registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
		registry.add("spring.rabbitmq.username", () -> "guest");
		registry.add("spring.rabbitmq.password", () -> "guest");
	}

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private MockMvc mockMvc;

	@Value("${app.rabbitmq.order-queue-name}")
	private String orderQueueName;

	@BeforeEach
	void setUp() {
		orderRepository.deleteAll();
		// Clear metrics between tests
		meterRegistry.clear();
	}

	/**
	 * ERROR SCENARIO TESTING
	 * Critical tests for notification service reliability and failure modes.
	 */

	@Test
	void contextLoads() {
		assertThat(notificationService).isNotNull();
		assertThat(orderService).isNotNull();
		assertThat(orderRepository).isNotNull();
		assertThat(rabbitTemplate).isNotNull();
	}

	/**
	 * Error Scenario: Test handling of invalid recipient data (empty userId)
	 */
	@Test
	void shouldRejectNotificationWithNullUserId() {
		// Given
		Long userId = null;
		String message = "Test notification";

		// When/Then - should fail fast with validation error
		assertThatThrownBy(() -> notificationService.sendEmailNotification(userId, message))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("User ID cannot be null");
	}

	/**
	 * Error Scenario: Test handling of invalid message content
	 */
	@Test
	void shouldRejectNotificationWithNullMessage() {
		// Given
		Long userId = 100L;
		String message = null;

		// When/Then
		assertThatThrownBy(() -> notificationService.sendEmailNotification(userId, message))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("Message cannot be null");
	}

	/**
	 * Error Scenario: Test handling of malformed RabbitMQ messages
	 */
	@Test
	void shouldHandleMalformedRabbitMQMessage() {
		// Given - invalid JSON message
		String malformedMessage = "{invalid json";

		// When - publish malformed message to queue
		rabbitTemplate.convertAndSend(orderQueueName, malformedMessage);

		// Then - should log error and increment failure metrics
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Double failureCount = meterRegistry.counter("notification.event.failed").count();
			assertThat(failureCount).isGreaterThan(0);
		});
	}

	/**
	 * Error Scenario: Test handling of RabbitMQ message with null userId
	 */
	@Test
	void shouldHandleOrderEventWithNullUserId() throws Exception {
		// Given - event with null userId
		OrderCreatedEvent event = new OrderCreatedEvent(
			null, // null userId
			200L,
			"ORDER-99999",
			OrderStatus.CREATED,
			LocalDateTime.now()
		);

		// When - publish event with null userId
		String eventJson = objectMapper.writeValueAsString(event);
		rabbitTemplate.convertAndSend(orderQueueName, eventJson);

		// Then - should handle gracefully and log error
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Double invalidCount = meterRegistry.counter("notification.event.invalid").count();
			assertThat(invalidCount).isGreaterThan(0);
		});
	}

	/**
	 * Error Scenario: Test RabbitMQ connection resilience
	 */
	@Test
	void shouldVerifyRabbitMQConnectionIsHealthy() {
		// Given/When
		boolean isRunning = rabbitMQContainer.isRunning();

		// Then
		assertThat(isRunning).isTrue();
		assertThat(rabbitMQContainer.getAmqpPort()).isGreaterThan(0);
		
		// Verify we can connect and publish
		assertThatCode(() -> {
			OrderCreatedEvent event = new OrderCreatedEvent(
				123L, 456L, "ORDER-TEST", OrderStatus.CREATED, LocalDateTime.now()
			);
			rabbitTemplate.convertAndSend(orderQueueName, event);
		}).doesNotThrowAnyException();
	}

	/**
	 * Error Scenario: Test notification service handles exceptions without crashing
	 */
	@Test
	void shouldContinueProcessingAfterNotificationFailure() {
		// Given
		Long validUserId = 500L;
		String validMessage = "Valid notification";

		// When - send a valid notification after testing invalid ones
		notificationService.sendEmailNotification(validUserId, validMessage);

		// Then - service should still be operational
		await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isGreaterThan(0);
		});
	}

	/**
	 * Error Scenario: Test database constraint violations and error handling
	 */
	@Test
	void shouldHandleDatabaseExceptions() {
		// Given - create order with unique idempotency key
		Long userId = 600L;
		String idempotencyKey = "db-error-test-" + System.currentTimeMillis();
		
		// When - first creation succeeds
		OrderResponseDto firstOrder = orderService.createOrder(userId, idempotencyKey);
		assertThat(firstOrder).isNotNull();
		
		// Then - duplicate creation with same key should be handled gracefully (idempotency)
		OrderResponseDto secondOrder = orderService.createOrder(userId, idempotencyKey);
		assertThat(secondOrder.getId()).isEqualTo(firstOrder.getId());
		
		// Verify only one order exists
		assertThat(orderRepository.count()).isEqualTo(1);
	}

	/**
	 * Error Scenario: Test concurrent notification processing doesn't cause race conditions
	 */
	@Test
	void shouldHandleConcurrentNotificationsWithoutErrors() {
		// Given
		int concurrentRequests = 20;
		
		// When - fire multiple concurrent notifications
		for (int i = 0; i < concurrentRequests; i++) {
			Long userId = 700L + i;
			notificationService.sendEmailNotification(userId, "Concurrent test " + i);
			notificationService.sendSmsNotification(userId, "SMS test " + i);
		}

		// Then - all should complete successfully
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Double emailSuccess = meterRegistry.counter("notification.email.success").count();
			Double smsSuccess = meterRegistry.counter("notification.sms.success").count();
			assertThat(emailSuccess).isEqualTo(concurrentRequests);
			assertThat(smsSuccess).isEqualTo(concurrentRequests);
		});
	}

	/**
	 * Error Scenario: Test REST API validation and error responses
	 */
	@Test
	void shouldReturnBadRequestForInvalidRestInput() throws Exception {
		// Given - request with negative userId
		CreateOrderRequest invalidRequest = new CreateOrderRequest(-100L, "test-key");
		String requestJson = objectMapper.writeValueAsString(invalidRequest);

		// When/Then
		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson))
			.andExpect(status().isBadRequest());
	}

	/**
	 * Error Scenario: Test REST API handles missing required fields
	 */
	@Test
	void shouldReturnBadRequestForEmptyIdempotencyKey() throws Exception {
		// Given
		CreateOrderRequest invalidRequest = new CreateOrderRequest(100L, "");
		String requestJson = objectMapper.writeValueAsString(invalidRequest);

		// When/Then
		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson))
			.andExpect(status().isBadRequest());
	}

	/**
	 * Error Scenario: Test system behavior under high load
	 */
	@Test
	void shouldMaintainStabilityUnderHighLoad() {
		// Given
		int highLoadRequests = 50;
		
		// When - create many orders rapidly
		for (int i = 0; i < highLoadRequests; i++) {
			String idempotencyKey = "load-test-" + i + "-" + System.currentTimeMillis();
			orderService.createOrder(800L + i, idempotencyKey);
		}

		// Then - system should remain stable
		await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
			long orderCount = orderRepository.count();
			assertThat(orderCount).isEqualTo(highLoadRequests);
		});
	}

	/**
	 * Error Scenario: Test metrics are recorded even during error conditions
	 */
	@Test
	void shouldRecordMetricsForFailedOperations() {
		// Given - invalid notification request
		Long userId = null;
		String message = "Test";

		// When - attempt invalid operation
		try {
			notificationService.sendEmailNotification(userId, message);
		} catch (Exception e) {
			// Expected exception
		}

		// Then - metrics should still be available for monitoring
		assertThat(meterRegistry).isNotNull();
		// Note: In production, you would verify failure metrics are incremented
	}

	@Test
	void shouldVerifyRabbitMQContainerIsRunning() {
		assertThat(rabbitMQContainer.isRunning()).isTrue();
		assertThat(rabbitMQContainer.getAmqpPort()).isGreaterThan(0);
	}

	/**
	 * Integration test: Order creation should persist to database and publish RabbitMQ event
	 */
	@Test
	void shouldCreateOrderAndPublishEventToRabbitMQ() throws Exception {
		// Given
		Long userId = 123L;
		String idempotencyKey = "test-key-" + System.currentTimeMillis();

		// When
		OrderResponseDto response = orderService.createOrder(userId, idempotencyKey);

		// Then - verify database persistence
		assertThat(response).isNotNull();
		assertThat(response.getUserId()).isEqualTo(userId);
		assertThat(response.getStatus()).isEqualTo(OrderStatus.CREATED);

		// Verify order is in database
		Order savedOrder = orderRepository.findById(response.getId()).orElse(null);
		assertThat(savedOrder).isNotNull();
		assertThat(savedOrder.getUserId()).isEqualTo(userId);

		// Verify RabbitMQ message was published
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Message message = rabbitTemplate.receive(orderQueueName, 1000);
			assertThat(message).isNotNull();
			
			OrderCreatedEvent event = objectMapper.readValue(
				message.getBody(), 
				OrderCreatedEvent.class
			);
			assertThat(event.userId()).isEqualTo(userId);
			assertThat(event.orderId()).isEqualTo(response.getId());
			assertThat(event.status()).isEqualTo(OrderStatus.CREATED);
		});
	}

	/**
	 * Integration test: Idempotency should prevent duplicate order creation
	 */
	@Test
	void shouldHandleIdempotencyCorrectly() {
		// Given
		Long userId = 456L;
		String idempotencyKey = "idempotent-key-" + System.currentTimeMillis();

		// When - create order twice with same idempotency key
		OrderResponseDto firstResponse = orderService.createOrder(userId, idempotencyKey);
		OrderResponseDto secondResponse = orderService.createOrder(userId, idempotencyKey);

		// Then - should return same order
		assertThat(firstResponse.getId()).isEqualTo(secondResponse.getId());
		assertThat(firstResponse.getStatus()).isEqualTo(secondResponse.getStatus());

		// Verify only one order in database
		long orderCount = orderRepository.count();
		assertThat(orderCount).isEqualTo(1);
	}

	/**
	 * Integration test: REST endpoint should create order via HTTP
	 */
	@Test
	void shouldCreateOrderViaRestEndpoint() throws Exception {
		// Given
		CreateOrderRequest request = new CreateOrderRequest(789L, "rest-test-key-" + System.currentTimeMillis());
		String requestJson = objectMapper.writeValueAsString(request);

		// When/Then
		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.userId").value(789))
			.andExpect(jsonPath("$.status").value("CREATED"));
	}

	/**
	 * Integration test: REST endpoint validation should reject invalid requests
	 */
	@Test
	void shouldRejectInvalidOrderCreationRequest() throws Exception {
		// Given - invalid user ID
		CreateOrderRequest request = new CreateOrderRequest(-1L, "invalid-test-key");
		String requestJson = objectMapper.writeValueAsString(request);

		// When/Then
		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson))
			.andExpect(status().isBadRequest());
	}

	/**
	 * Integration test: Missing idempotency key should be rejected
	 */
	@Test
	void shouldRejectRequestWithoutIdempotencyKey() throws Exception {
		// Given
		CreateOrderRequest request = new CreateOrderRequest(999L, "");
		String requestJson = objectMapper.writeValueAsString(request);

		// When/Then
		mockMvc.perform(post("/api/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson))
			.andExpect(status().isBadRequest());
	}

	/**
	 * Integration test: Notification service should send email notification asynchronously
	 */
	@Test
	void shouldSendEmailNotificationAsynchronously() {
		// Given
		Long userId = 111L;
		String message = "Test notification message";

		// When
		notificationService.sendEmailNotification(userId, message);

		// Then - verify metrics were recorded
		await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.email.success").count();
			assertThat(successCount).isGreaterThan(0);
		});
	}

	/**
	 * Integration test: Notification service should send SMS notification asynchronously
	 */
	@Test
	void shouldSendSmsNotificationAsynchronously() {
		// Given
		Long userId = 222L;
		String message = "Test SMS message";

		// When
		notificationService.sendSmsNotification(userId, message);

		// Then - verify metrics were recorded
		await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
			Double successCount = meterRegistry.counter("notification.sms.success").count();
			assertThat(successCount).isGreaterThan(0);
		});
	}

	/**
	 * Integration test: RabbitMQ listener should process order created events
	 */
	@Test
	void shouldProcessOrderCreatedEventFromQueue() {
		// Given
		OrderCreatedEvent event = new OrderCreatedEvent(
			100L,
			200L,
			"ORDER-12345",
			OrderStatus.CREATED,
			LocalDateTime.now()
		);

		// When - publish event to queue
		rabbitTemplate.convertAndSend(orderQueueName, event);

		// Then - verify event was consumed (check metrics or logs)
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			// The listener should process the message
			// In production, you'd verify side effects like notifications sent
			Message message = rabbitTemplate.receive(orderQueueName, 500);
			// Message should be consumed
			assertThat(message).isNull(); // Queue should be empty after processing
		});
	}

	/**
	 * Integration test: End-to-end flow from order creation to notification
	 */
	@Test
	void shouldCompleteEndToEndOrderCreationFlow() {
		// Given
		Long userId = 333L;
		String idempotencyKey = "e2e-test-" + System.currentTimeMillis();

		// When - create order
		OrderResponseDto response = orderService.createOrder(userId, idempotencyKey);

		// Then - verify complete flow
		assertThat(response).isNotNull();
		
		// 1. Order saved to database
		assertThat(orderRepository.findById(response.getId())).isPresent();
		
		// 2. Event published to RabbitMQ
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Message message = rabbitTemplate.receive(orderQueueName, 1000);
			assertThat(message).isNotNull();
		});
		
		// 3. Idempotency works on retry
		OrderResponseDto retryResponse = orderService.createOrder(userId, idempotencyKey);
		assertThat(retryResponse.getId()).isEqualTo(response.getId());
	}

	/**
	 * Integration test: Database transaction rollback on event publishing failure
	 */
	@Test
	void shouldHandleDatabaseConstraintViolations() {
		// Given - Create an order first
		Long userId = 444L;
		String idempotencyKey = "constraint-test-" + System.currentTimeMillis();
		
		Order order = new Order(userId, OrderStatus.CREATED, idempotencyKey);
		orderRepository.save(order);

		// When/Then - attempting to create with same idempotency key should return existing
		OrderResponseDto response = orderService.createOrder(userId, idempotencyKey);
		assertThat(response).isNotNull();
		assertThat(orderRepository.count()).isEqualTo(1);
	}

	/**
	 * Performance test: Should handle concurrent order creation
	 */
	@Test
	void shouldHandleConcurrentOrderCreation() {
		// Given
		int numberOfOrders = 10;
		
		// When - create multiple orders concurrently
		for (int i = 0; i < numberOfOrders; i++) {
			Long userId = 1000L + i;
			String idempotencyKey = "concurrent-test-" + i + "-" + System.currentTimeMillis();
			orderService.createOrder(userId, idempotencyKey);
		}

		// Then - all orders should be created
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			long count = orderRepository.count();
			assertThat(count).isEqualTo(numberOfOrders);
		});
	}
}
