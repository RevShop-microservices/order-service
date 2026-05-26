package com.example.order_service.CustomExceptions;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message){
        super(message);
    }
}
