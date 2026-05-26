package com.example.order_service.repository;

import com.example.order_service.models.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {
    List<Payout> findBySellerId(Long sellerId);
    List<Payout> findByStatus(String status);
}
