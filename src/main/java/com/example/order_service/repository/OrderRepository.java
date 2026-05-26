package com.example.order_service.repository;

import com.example.order_service.models.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Order findByOrderId(Long orderId);
    List<Order> findByUserId(Long userId);
}
