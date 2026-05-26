package com.example.order_service.models;

public enum OrderStatus {
    PLACED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURN_APPROVED,
    RETURN_DENIED,
    REFUNDED
}