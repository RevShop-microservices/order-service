package com.example.order_service.dto;

import lombok.Data;

@Data
public class BuyNowRequestDTO {
    private Long userId;
    private String productId;
    private int quantity;
    private Long addressId;
    private String couponCode;
}
