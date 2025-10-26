package com.notification.notification.service;

import com.notification.notification.dto.OrderResponseDto;
import com.notification.notification.entity.Order;
import com.notification.notification.enums.OrderStatus;
import com.notification.notification.event.OrderCreatedEvent;
import com.notification.notification.exception.OrderCreationException;
import com.notification.notification.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for OrderService.
 * Tests order creation, event publishing, idempotency, error handling, and RabbitMQ integration.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

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
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Value("${app.rabbitmq.order-queue-name}")
	private String orderQueueName;

	@BeforeEach
	void setUp() {
		// Clean up before each test
		orderRepository.deleteAll();
		
		// Purge RabbitMQ queue
		rabbitTemplate.execute(channel -> {
			channel.queuePurge(orderQueueName);
			return null;
		});
	}

	@Test
	void shouldCreateOrderSuccessfully() {
		// Given
		Long userId = 100L;
		String idempotencyKey = "test-order-" + System.currentTimeMillis();

		// When
		OrderResponseDto response = orderService.createOrder(userId, idempotencyKey);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getId()).isNotNull();
		assertThat(response.getUserId()).isEqualTo(userId);
		assertThat(response.getStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(response.getCreatedAt()).isNotNull();
	}

	@Test
	void shouldPersistOrderToDatabase() {
		// Given
		Long userId = 101L;
		String idempotencyKey = "persist-test-" + System.currentTimeMillis();

		// When
		OrderResponseDto response = orderService.createOrder(userId, idempotencyKey);

		// Then - verify database persistence
		Order savedOrder = orderRepository.findById(response.getId()).orElse(null);
		assertThat(savedOrder).isNotNull();
		assertThat(savedOrder.getUserId()).isEqualTo(userId);
		assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(savedOrder.getIdempotencyKey()).isEqualTo(idempotencyKey);
	}

	@Test
	void shouldPublishEventToRabbitMQ() throws Exception {
		// Given
		Long userId = 102L;
		String idempotencyKey = "rabbitmq-test-" + System.currentTimeMillis();

		// When
		OrderResponseDto response = orderService.createOrder(userId, idempotencyKey);

		// Then - verify RabbitMQ event
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

	@Test
	void shouldHandleIdempotencyCorrectly() {
		// Given
		Long userId = 103L;
		String idempotencyKey = "idempotency-test-" + System.currentTimeMillis();

		// When - create same order twice
		OrderResponseDto firstResponse = orderService.createOrder(userId, idempotencyKey);
		OrderResponseDto secondResponse = orderService.createOrder(userId, idempotencyKey);

		// Then - should return same order
		assertThat(firstResponse.getId()).isEqualTo(secondResponse.getId());
		assertThat(firstResponse.getUserId()).isEqualTo(secondResponse.getUserId());

		// Verify only one order in database
		List<Order> orders = orderRepository.findAll();
		assertThat(orders).hasSize(1);
	}

	@Test
	void shouldRejectNullUserId() {
		// Given
		Long userId = null;
		String idempotencyKey = "null-user-test";

		// When/Then
		assertThatThrownBy(() -> orderService.createOrder(userId, idempotencyKey))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("User ID cannot be null");
	}

	@Test
	void shouldRejectNullIdempotencyKey() {
		// Given
		Long userId = 104L;
		String idempotencyKey = null;

		// When/Then
		assertThatThrownBy(() -> orderService.createOrder(userId, idempotencyKey))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("Idempotency key cannot be null");
	}

	@Test
	void shouldRejectInvalidUserId() {
		// Given
		Long userId = -1L;
		String idempotencyKey = "invalid-user-test";

		// When/Then
		assertThatThrownBy(() -> orderService.createOrder(userId, idempotencyKey))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("User ID must be positive");
	}

	@Test
	void shouldRejectZeroUserId() {
		// Given
		Long userId = 0L;
		String idempotencyKey = "zero-user-test";

		// When/Then
		assertThatThrownBy(() -> orderService.createOrder(userId, idempotencyKey))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("User ID must be positive");
	}

	@Test
	void shouldHandleConcurrentOrderCreation() {
		// Given
		int numberOfOrders = 10;

		// When - create multiple orders
		for (int i = 0; i < numberOfOrders; i++) {
			Long userId = 200L + i;
			String idempotencyKey = "concurrent-" + i + "-" + System.currentTimeMillis();
			orderService.createOrder(userId, idempotencyKey);
		}

		// Then
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			List<Order> orders = orderRepository.findAll();
			assertThat(orders).hasSize(numberOfOrders);
		});
	}

	@Test
	void shouldCreateOrdersForDifferentUsers() {
		// Given
		Long user1 = 301L;
		Long user2 = 302L;
		String key1 = "user1-order-" + System.currentTimeMillis();
		String key2 = "user2-order-" + System.currentTimeMillis();

		// When
		OrderResponseDto order1 = orderService.createOrder(user1, key1);
		OrderResponseDto order2 = orderService.createOrder(user2, key2);

		// Then
		assertThat(order1.getId()).isNotEqualTo(order2.getId());
		assertThat(order1.getUserId()).isEqualTo(user1);
		assertThat(order2.getUserId()).isEqualTo(user2);

		List<Order> orders = orderRepository.findAll();
		assertThat(orders).hasSize(2);
	}

	@Test
	void shouldCreateMultipleOrdersForSameUserWithDifferentKeys() {
		// Given
		Long userId = 400L;
		String key1 = "order1-" + System.currentTimeMillis();
		String key2 = "order2-" + (System.currentTimeMillis() + 1);

		// When
		OrderResponseDto order1 = orderService.createOrder(userId, key1);
		OrderResponseDto order2 = orderService.createOrder(userId, key2);

		// Then
		assertThat(order1.getId()).isNotEqualTo(order2.getId());
		assertThat(order1.getUserId()).isEqualTo(userId);
		assertThat(order2.getUserId()).isEqualTo(userId);

		List<Order> orders = orderRepository.findAll();
		assertThat(orders).hasSize(2);
	}

	@Test
	void shouldPublishMultipleEventsToRabbitMQ() throws Exception {
		// Given
		int numberOfOrders = 3;

		// When
		for (int i = 0; i < numberOfOrders; i++) {
			Long userId = 500L + i;
			String idempotencyKey = "multi-event-" + i + "-" + System.currentTimeMillis();
			orderService.createOrder(userId, idempotencyKey);
		}

		// Then - verify all events were published
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			int eventCount = 0;
			for (int i = 0; i < numberOfOrders; i++) {
				Message message = rabbitTemplate.receive(orderQueueName, 1000);
				if (message != null) {
					eventCount++;
					OrderCreatedEvent event = objectMapper.readValue(
						message.getBody(),
						OrderCreatedEvent.class
					);
					assertThat(event).isNotNull();
					assertThat(event.status()).isEqualTo(OrderStatus.CREATED);
				}
			}
			assertThat(eventCount).isEqualTo(numberOfOrders);
		});
	}

	@Test
	void shouldNotPublishDuplicateEventsForIdempotentRequests() throws Exception {
		// Given
		Long userId = 600L;
		String idempotencyKey = "no-duplicate-event-" + System.currentTimeMillis();

		// When - create same order twice
		orderService.createOrder(userId, idempotencyKey);
		orderService.createOrder(userId, idempotencyKey);

		// Then - should only have one event in queue
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Message firstMessage = rabbitTemplate.receive(orderQueueName, 1000);
			assertThat(firstMessage).isNotNull();
			
			Message secondMessage = rabbitTemplate.receive(orderQueueName, 500);
			assertThat(secondMessage).isNull(); // No duplicate event
		});
	}

	@Test
	void shouldMaintainDataConsistencyBetweenDatabaseAndMessageQueue() throws Exception {
		// Given
		Long userId = 700L;
		String idempotencyKey = "consistency-test-" + System.currentTimeMillis();

		// When
		OrderResponseDto response = orderService.createOrder(userId, idempotencyKey);

		// Then - verify database and message queue consistency
		Order dbOrder = orderRepository.findById(response.getId()).orElse(null);
		assertThat(dbOrder).isNotNull();

		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Message message = rabbitTemplate.receive(orderQueueName, 1000);
			assertThat(message).isNotNull();
			
			OrderCreatedEvent event = objectMapper.readValue(
				message.getBody(),
				OrderCreatedEvent.class
			);
			
			// Verify event data matches database record
			assertThat(event.orderId()).isEqualTo(dbOrder.getId());
			assertThat(event.userId()).isEqualTo(dbOrder.getUserId());
			assertThat(event.status()).isEqualTo(dbOrder.getStatus());
		});
	}
}
