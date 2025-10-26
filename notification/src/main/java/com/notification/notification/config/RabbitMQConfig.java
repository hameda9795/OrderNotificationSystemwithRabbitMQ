package com.notification.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * RabbitMQ configuration with proper error handling, retry logic, and environment profiles.
 * Includes exchanges, queues, bindings, and connection management.
 */
@Configuration
@Profile("!test")
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Value("${app.rabbitmq.order-queue-name:order-queue}")
    private String orderQueueName;

    @Value("${app.rabbitmq.order-exchange-name:order.exchange}")
    private String orderExchangeName;

    @Value("${app.rabbitmq.order-routing-key:order.created}")
    private String orderRoutingKey;

    @Value("${app.rabbitmq.dlx-name:order.dlx}")
    private String deadLetterExchange;

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
    public Binding orderBinding() {
        return BindingBuilder
            .bind(orderQueue())
            .to(orderExchange())
            .with(orderRoutingKey);
    }

    /**
     * Binding for dead letter queue.
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
            .bind(deadLetterQueue())
            .to(deadLetterExchange())
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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setRetryTemplate(retryTemplate());
        template.setMandatory(true);
        
        // Confirm callback for publisher confirms
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Message failed to deliver: {}", cause);
            }
        });
        
        // Returns callback for unroutable messages
        template.setReturnsCallback(returned -> {
            log.error("Message returned: {}", returned.getReplyText());
        });
        
        return template;
    }

    /**
     * Retry template with exponential backoff.
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /**
     * Connection factory with proper configuration and publisher confirms enabled.
     */
    @Bean
    public ConnectionFactory connectionFactory(
        @Value("${spring.rabbitmq.host:localhost}") String host,
        @Value("${spring.rabbitmq.port:5672}") int port,
        @Value("${spring.rabbitmq.username}") String username,
        @Value("${spring.rabbitmq.password}") String password
    ) {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setChannelCacheSize(25);
        factory.setConnectionTimeout(5000);
        factory.setRequestedHeartBeat(30);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
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
