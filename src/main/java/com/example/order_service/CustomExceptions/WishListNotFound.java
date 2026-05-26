package com.example.order_service.CustomExceptions;

public class WishListNotFound extends RuntimeException{
    public WishListNotFound(String message){
        super(message);
    }
}
