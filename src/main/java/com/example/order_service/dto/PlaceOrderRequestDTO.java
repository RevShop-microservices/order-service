package com.example.order_service.dto;

import lombok.Data;

@Data
public class PlaceOrderRequestDTO {
    private Long userId;
    private Long addressId;
    private String couponCode;
}