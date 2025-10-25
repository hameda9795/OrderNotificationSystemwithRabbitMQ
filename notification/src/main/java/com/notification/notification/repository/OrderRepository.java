package com.notification.notification.repository;

import com.notification.notification.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import com.notification.notification.entity.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Order entities.
 * Provides custom query methods for business-specific order retrieval operations.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
	
	/**
	 * Find all orders for a specific user.
	 * @param userId The ID of the user
	 * @return List of orders for the user
	 */
	List<Order> findByUserId(Long userId);
	
	/**
	 * Find orders by status.
	 * @param status The order status
	 * @return List of orders with the specified status
	 */
	List<Order> findByStatus(OrderStatus status);
	
	/**
	 * Find an order by its order number.
	 * @param orderNumber The order number
	 * @return Optional containing the order if found
	 */
	Optional<Order> findByOrderNumber(String orderNumber);
	
	/**
	 * Find orders created within a date range.
	 * @param startDate Start of the date range
	 * @param endDate End of the date range
	 * @return List of orders created within the range
	 */
	List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
	
	/**
	 * Find orders by user and status.
	 * @param userId The user ID
	 * @param status The order status
	 * @return List of orders matching the criteria
	 */
	List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
	
	/**
	 * Count orders for a specific user.
	 * @param userId The user ID
	 * @return Number of orders for the user
	 */
	long countByUserId(Long userId);
}

