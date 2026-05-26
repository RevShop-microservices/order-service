package com.example.order_service.CustomExceptions;

public class QuantityExceedStockException extends RuntimeException{
    public QuantityExceedStockException(String message){
        super(message);
    }
}
