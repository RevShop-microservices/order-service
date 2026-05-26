package com.example.order_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemDTO {
    private String productId;
    private String name;
    private Double price;
    private Integer quantity;
    private Double totalPrice;
}