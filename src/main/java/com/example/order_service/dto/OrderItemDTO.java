package com.example.order_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemDTO {

    private String productId;
    private String productName;
    private Integer quantity;
    private Double price;

    private Double totalPrice;

    private boolean cancelled;
}