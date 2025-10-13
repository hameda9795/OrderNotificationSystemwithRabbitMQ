package com.notification.notification.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public void sendEmailNotification(Long userId, String message) {
        System.out.println("Email sent to user " + userId + " with message: " + message);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void sendSmsNotification(Long userId, String message) {
        System.out.println("SMS sent to user " + userId + " with message: " + message);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @RabbitListener(queues = "order-queue")
    public void handleOrderCreatedEvent(String message) {
        Long userId = Long.parseLong(message);
        sendEmailNotification(userId, "Your order has been created!");
        sendSmsNotification(userId, "Your order has been created!");
    }
}