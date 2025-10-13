package com.notification.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.notification.notification.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
	
}
