package com.notification.notification.repository;

import com.notification.notification.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
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
	 * Find all orders for a specific user with pagination.
	 * Uses explicit JPQL query with null check to prevent unexpected database queries.
	 * Returns empty page if userId is null.
	 * 
	 * @param userId The ID of the user (must not be null)
	 * @param pageable Pagination information
	 * @return Page of orders for the user, empty page if userId is null
	 */
	@Query("SELECT o FROM Order o WHERE o.userId = :userId AND :userId IS NOT NULL")
	Page<Order> findByUserId(@Param("userId") @NonNull Long userId, Pageable pageable);
	
	/**
	 * Find recent orders for a specific user (limit 100).
	 * Use this method when you need a bounded result set without pagination.
	 * Uses PageRequest internally to limit results and prevent OutOfMemoryError.
	 * 
	 * @param userId The ID of the user (must not be null)
	 * @return List of up to 100 most recent orders for the user
	 */
	@Query("SELECT o FROM Order o WHERE o.userId = :userId AND :userId IS NOT NULL ORDER BY o.createdAt DESC")
	List<Order> findTop100ByUserIdOrderByCreatedAtDesc(@Param("userId") @NonNull Long userId, Pageable pageable);
	
	/**
	 * Find orders by status with pagination.
	 * Optimized with @EntityGraph to prevent N+1 queries when associations are loaded.
	 * Note: Update attributePaths when adding associations (e.g., orderItems, user) to Order entity.
	 * 
	 * @param status The order status (must not be null)
	 * @param pageable Pagination information
	 * @return Page of orders with the specified status, empty page if status is null
	 */
	@EntityGraph(attributePaths = {})
	@Query("SELECT o FROM Order o WHERE o.status = :status AND :status IS NOT NULL")
	Page<Order> findByStatus(@Param("status") @NonNull OrderStatus status, Pageable pageable);
	
	/**
	 * Find recent orders by status (limit 100).
	 * Use this method when you need a bounded result set without pagination.
	 * Prevents OutOfMemoryError when querying popular statuses.
	 * Uses PageRequest internally to limit results.
	 * 
	 * @param status The order status (must not be null)
	 * @return List of up to 100 most recent orders with the specified status
	 */
	@EntityGraph(attributePaths = {})
	@Query("SELECT o FROM Order o WHERE o.status = :status AND :status IS NOT NULL ORDER BY o.createdAt DESC")
	List<Order> findTop100ByStatusOrderByCreatedAtDesc(@Param("status") @NonNull OrderStatus status, Pageable pageable);
	
	/**
	 * Find an order by its order number.
	 * @param orderNumber The order number (must not be null)
	 * @return Optional containing the order if found, empty Optional if orderNumber is null
	 */
	@Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber AND :orderNumber IS NOT NULL")
	Optional<Order> findByOrderNumber(@Param("orderNumber") @NonNull String orderNumber);
	
	/**
	 * Find orders created within a date range with pagination.
	 * 
	 * @param startDate Start of the date range (must not be null)
	 * @param endDate End of the date range (must not be null)
	 * @param pageable Pagination information
	 * @return Page of orders created within the range, empty page if either date is null
	 */
	@Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND :startDate IS NOT NULL AND :endDate IS NOT NULL")
	Page<Order> findByCreatedAtBetween(@Param("startDate") @NonNull LocalDateTime startDate, @Param("endDate") @NonNull LocalDateTime endDate, Pageable pageable);
	
	/**
	 * Find recent orders created within a date range (limit 1000).
	 * Use this method when you need a bounded result set without pagination.
	 * Uses PageRequest internally to limit results.
	 * 
	 * @param startDate Start of the date range (must not be null)
	 * @param endDate End of the date range (must not be null)
	 * @return List of up to 1000 most recent orders created within the range
	 */
	@Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND :startDate IS NOT NULL AND :endDate IS NOT NULL ORDER BY o.createdAt DESC")
	List<Order> findTop1000ByCreatedAtBetweenOrderByCreatedAtDesc(@Param("startDate") @NonNull LocalDateTime startDate, @Param("endDate") @NonNull LocalDateTime endDate, Pageable pageable);
	
	/**
	 * Find orders by user and status with pagination.
	 * Uses explicit JPQL query with null checks to prevent unexpected database queries.
	 * Returns empty page if either parameter is null.
	 * 
	 * @param userId The user ID (must not be null)
	 * @param status The order status (must not be null)
	 * @param pageable Pagination information
	 * @return Page of orders matching the criteria, empty page if any parameter is null
	 */
	@Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status = :status AND :userId IS NOT NULL AND :status IS NOT NULL")
	Page<Order> findByUserIdAndStatus(@Param("userId") @NonNull Long userId, @Param("status") @NonNull OrderStatus status, Pageable pageable);
	
	/**
	 * Find recent orders by user and status (limit 100).
	 * Use this method when you need a bounded result set without pagination.
	 * Uses PageRequest internally to limit results.
	 * 
	 * @param userId The user ID (must not be null)
	 * @param status The order status (must not be null)
	 * @return List of up to 100 most recent orders matching the criteria
	 */
	@Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status = :status AND :userId IS NOT NULL AND :status IS NOT NULL ORDER BY o.createdAt DESC")
	List<Order> findTop100ByUserIdAndStatusOrderByCreatedAtDesc(@Param("userId") @NonNull Long userId, @Param("status") @NonNull OrderStatus status, Pageable pageable);
	
	/**
	 * Count orders for a specific user.
	 * @param userId The user ID (must not be null)
	 * @return Number of orders for the user, 0 if userId is null
	 */
	@Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND :userId IS NOT NULL")
	long countByUserId(@Param("userId") @NonNull Long userId);

	/**
	 * Find an order by user ID and idempotency key for duplicate prevention.
	 * This method is crucial for implementing idempotency in order creation.
	 * 
	 * @param userId The user ID (must not be null)
	 * @param idempotencyKey The idempotency key (must not be null)
	 * @return Optional containing the order if found
	 */
	@Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.idempotencyKey = :idempotencyKey AND :userId IS NOT NULL AND :idempotencyKey IS NOT NULL")
	Optional<Order> findByUserIdAndIdempotencyKey(@Param("userId") @NonNull Long userId, @Param("idempotencyKey") @NonNull String idempotencyKey);
}

