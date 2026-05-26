package com.example.order_service.CustomExceptions;

public class OrderCancelAfterShippingEXception extends RuntimeException{
    public OrderCancelAfterShippingEXception(String message){
        super(message);
    }
}
