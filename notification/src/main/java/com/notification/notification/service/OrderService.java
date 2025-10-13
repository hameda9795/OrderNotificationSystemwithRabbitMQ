package com.notification.notification.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.notification.notification.repository.OrderRepository;
import com.notification.notification.entity.Order;
import java.time.LocalDateTime;

@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    
    public OrderService(OrderRepository orderRepository,
                        RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
    }
    
    public Order createOrder(Long userId) {
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        
       
        rabbitTemplate.convertAndSend("order-queue", userId.toString());
        
        return order;
    }
}