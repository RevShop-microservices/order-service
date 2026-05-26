package com.example.order_service.service;

import com.example.order_service.Clients.UserClient;
import com.example.order_service.CustomExceptions.InvalidRequestException;
import com.example.order_service.dto.NotificationRequest;
import com.example.order_service.dto.UserDTO;
import com.example.order_service.models.Payout;
import com.example.order_service.models.Products;
import com.example.order_service.repository.OrderItemRepository;
import com.example.order_service.repository.PayoutRepository;
import com.example.order_service.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired(required = false)
    private UserClient userClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Dynamic commission calculation based on stock and demand.
     * Low stock + high demand = higher commission (platform benefits from scarcity).
     * High stock + low demand = lower commission (encourages sellers to list).
     */
    public double calculateCommissionRate(Long sellerId) {
        // Get total units sold (demand indicator)
        Integer totalUnitsSold = Optional.ofNullable(
                orderItemRepository.getTotalUnitsSold(sellerId)).orElse(0);

        // Get total remaining stock
        List<Products> sellerProducts = productRepository.findBySellerId(sellerId);
        int totalStock = sellerProducts.stream()
                .mapToInt(p -> p.getStock() != null ? p.getStock() : 0)
                .sum();

        // Commission tiers based on demand-to-stock ratio
        if (totalStock == 0 && totalUnitsSold == 0) {
            return 5.0; // New sellers get lowest commission
        }

        double demandRatio = totalStock > 0
                ? (double) totalUnitsSold / totalStock
                : totalUnitsSold;

        if (demandRatio >= 5.0) {
            return 15.0; // High demand, low stock → 15%
        } else if (demandRatio >= 2.0) {
            return 12.0; // Moderate-high demand → 12%
        } else if (demandRatio >= 1.0) {
            return 10.0; // Balanced → 10%
        } else if (demandRatio >= 0.5) {
            return 7.0;  // Low demand → 7%
        } else {
            return 5.0;  // Very low demand → 5%
        }
    }

    /**
     * Generate a payout record for a seller over a given period.
     */
    public Payout generatePayout(Long sellerId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        Double grossRevenue = Optional.ofNullable(
                orderItemRepository.getTotalRevenue(sellerId)).orElse(0.0);

        double commissionRate = calculateCommissionRate(sellerId);
        double commissionAmount = grossRevenue * commissionRate / 100.0;
        double netPayout = grossRevenue - commissionAmount;

        Payout payout = Payout.builder()
                .sellerId(sellerId)
                .grossAmount(grossRevenue)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .netPayout(netPayout)
                .status("PENDING")
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .build();

        Payout saved = payoutRepository.save(payout);
        log.info("Payout generated for seller {}: gross={}, commission={}%, net={}", sellerId, grossRevenue, commissionRate, netPayout);
        return saved;
    }

    public Payout markAsPaid(Long payoutId) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new InvalidRequestException("Payout not found"));

        if ("PAID".equals(payout.getStatus())) {
            throw new InvalidRequestException("Payout is already marked as paid");
        }

        payout.setStatus("PAID");
        Payout saved = payoutRepository.save(payout);

        // Notify seller
        try {
            if (userClient != null) {
                UserDTO seller = userClient.getUserById(payout.getSellerId());
                rabbitTemplate.convertAndSend("order-exchange", "order.notification", NotificationRequest.builder()
                        .userId(seller.getId())
                        .userEmail(seller.getEmail())
                        .subject("Payout Processed")
                        .message("Hello " + seller.getName() + ",\n\nYour payout of ₹" + String.format("%.2f", payout.getNetPayout())
                                + " has been processed.\nCommission Rate: " + payout.getCommissionRate() + "%\n\nThank you for selling on NexShop!")
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to notify seller about payout", e);
        }

        return saved;
    }

    public List<Payout> getPendingPayouts() {
        return payoutRepository.findByStatus("PENDING");
    }

    public List<Payout> getPayoutsBySeller(Long sellerId) {
        return payoutRepository.findBySellerId(sellerId);
    }

    public List<Payout> getAllPayouts() {
        return payoutRepository.findAll();
    }
}
