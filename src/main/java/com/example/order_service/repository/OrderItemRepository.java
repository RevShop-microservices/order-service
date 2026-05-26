package com.example.order_service.repository;

import com.example.order_service.models.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderOrderId(Long orderId);

    boolean existsByProductIdAndOrderUserIdAndIsCancelledFalse(String productId, Long userId);

    boolean existsByProductIdAndOrderUserId(String productId, Long userId);

    // Total units sold
    @Query("SELECT SUM(o.quantity) FROM OrderItem o WHERE o.sellerId = :sellerId")
    Integer getTotalUnitsSold(Long sellerId);

    // Total revenue
    @Query("SELECT SUM(o.price * o.quantity) FROM OrderItem o WHERE o.sellerId = :sellerId")
    Double getTotalRevenue(Long sellerId);

    // Total orders
    @Query("SELECT COUNT(DISTINCT o.order.orderId) FROM OrderItem o WHERE o.sellerId = :sellerId")
    Integer getTotalOrders(Long sellerId);

    // Top selling products
    @Query("SELECT o.productId, o.productName, SUM(o.quantity) as totalSold " +
            "FROM OrderItem o WHERE o.sellerId = :sellerId " +
            "GROUP BY o.productId, o.productName " +
            "ORDER BY totalSold DESC")
    List<Object[]> getTopSellingProducts(Long sellerId);
}