package com.example.order_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponseDTO {

    private Long orderId;
    private Long userId;

    private Double subtotal;
    private Double discount;
    private Double totalAmount;

    private String status;
    private LocalDateTime orderDate;

    private Long addressId;

    private List<OrderItemDTO> items;
}