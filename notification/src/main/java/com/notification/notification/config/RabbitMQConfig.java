package com.notification.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;

/**
 * RabbitMQ configuration with proper error handling, retry logic, and environment profiles.
 * Includes exchanges, queues, bindings, and connection management.
 */
@Configuration
@Profile("!test")
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    // Configuration constants
    private static final int DEFAULT_CHANNEL_CACHE_SIZE = 25;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;
    private static final int DEFAULT_CONCURRENT_CONSUMERS = 3;
    private static final int MAX_CONCURRENT_CONSUMERS = 10;
    private static final int PREFETCH_COUNT = 10;

    // Retry configuration constants
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_INTERVAL_MS = 1000;
    private static final double RETRY_MULTIPLIER = 2.0;
    private static final long MAX_RETRY_INTERVAL_MS = 10000;

    /** Order queue name - can be configured per environment */
    @Value("${app.rabbitmq.order-queue-name:order-queue}")
    private String orderQueueName;

    /** Order exchange name for topic-based routing */
    @Value("${app.rabbitmq.order-exchange-name:order.exchange}")
    private String orderExchangeName;

    /** Routing key for order creation events */
    @Value("${app.rabbitmq.order-routing-key:order.created}")
    private String orderRoutingKey;

    /** Dead letter exchange for failed messages */
    @Value("${app.rabbitmq.dlx-name:order.dlx}")
    private String deadLetterExchange;

    /** Dead letter queue name */
    @Value("${app.rabbitmq.dlq-name:order-queue.dlq}")
    private String deadLetterQueue;

    /**
     * Main order queue with dead letter exchange configuration.
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(orderQueueName)
            .withArgument("x-dead-letter-exchange", deadLetterExchange)
            .withArgument("x-dead-letter-routing-key", "order.dead")
            // TTL removed to match existing queue configuration
            .build();
    }

    /**
     * Dead letter queue for failed messages.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    /**
     * Topic exchange for order events.
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchangeName, true, false);
    }

    /**
     * Dead letter exchange.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(deadLetterExchange, true, false);
    }

    /**
     * Binding between order queue and exchange.
     */
    @Bean
    public Binding orderBinding(Queue orderQueue, TopicExchange orderExchange) {
        return BindingBuilder
            .bind(orderQueue)
            .to(orderExchange)
            .with(orderRoutingKey);
    }

    /**
     * Binding for dead letter queue.
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder
            .bind(deadLetterQueue)
            .to(deadLetterExchange)
            .with("order.dead");
    }

    /**
     * JSON message converter for proper serialization.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with retry configuration and error handling callbacks.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter messageConverter,
                                          RetryTemplate retryTemplate) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setRetryTemplate(retryTemplate);
        template.setMandatory(true);

        // Confirm callback for publisher confirms with comprehensive error handling
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Message failed to deliver. Correlation: {}, Cause: {}",
                    correlationData != null ? correlationData.getId() : "N/A", cause);
                // TODO: Implement recovery logic - send to monitoring/alerting system
                // e.g., messageFailureService.handleFailedMessage(correlationData, cause);
            } else {
                log.debug("Message successfully confirmed. Correlation: {}",
                    correlationData != null ? correlationData.getId() : "N/A");
            }
        });

        // Returns callback for unroutable messages
        template.setReturnsCallback(returned -> {
            log.error("Message returned: Exchange={}, RoutingKey={}, ReplyText={}, Message={}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyText(), returned.getMessage());
            // TODO: Implement recovery logic for returned messages
        });

        return template;
    }

    /**
     * Retry template with exponential backoff.
     * Configured with reasonable defaults for message delivery retries.
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Retry policy with configured maximum attempts
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_RETRY_ATTEMPTS);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Exponential backoff policy to avoid overwhelming the system
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_RETRY_INTERVAL_MS);
        backOffPolicy.setMultiplier(RETRY_MULTIPLIER);
        backOffPolicy.setMaxInterval(MAX_RETRY_INTERVAL_MS);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /**
     * Connection factory with proper configuration, validation, and publisher confirms enabled.
     * Validates required credentials and configures connection pooling.
     */
    @Bean
    public ConnectionFactory connectionFactory(
        @Value("${spring.rabbitmq.host:localhost}") String host,
        @Value("${spring.rabbitmq.port:5672}") int port,
        @Value("${spring.rabbitmq.username}") String username,
        @Value("${spring.rabbitmq.password}") String password
    ) {
        // Validate required credentials
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException(
                "RabbitMQ username and password must be provided. " +
                "Check spring.rabbitmq.username and spring.rabbitmq.password properties."
            );
        }

        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);

        // Connection pool configuration
        factory.setChannelCacheSize(DEFAULT_CHANNEL_CACHE_SIZE);
        factory.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        factory.setRequestedHeartBeat(HEARTBEAT_INTERVAL_SECONDS);

        // Publisher confirms and returns configuration
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);

        // Enable automatic recovery
        factory.getRabbitConnectionFactory().setAutomaticRecoveryEnabled(true);
        factory.getRabbitConnectionFactory().setNetworkRecoveryInterval(10000);

        // Set connection name for monitoring
        factory.getRabbitConnectionFactory().setConnectionName("notification-service");

        log.info("RabbitMQ connection factory configured for host: {}, port: {}", host, port);
        return factory;
    }

    /**
     * Listener container factory with error handling and retry configuration.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);
        
        // Error handler for listener exceptions
        factory.setErrorHandler(t -> {
            log.error("Error in message listener: {}", t.getMessage(), t);
        });
        
        return factory;
    }
}

/**
 * Test profile configuration for RabbitMQ.
 * Uses simpler configuration for testing environments.
 */
@Configuration
@Profile("test")
class TestRabbitMQConfig {

    @Bean
    public Queue orderQueue() {
        return new Queue("test-order-queue", false);
    }
}
