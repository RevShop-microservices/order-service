package com.example.order_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartResponseDTO {
    private Long userId;
    private List<CartItemDTO> items;
    private int totalItems;
    private Double totalPrice;
}