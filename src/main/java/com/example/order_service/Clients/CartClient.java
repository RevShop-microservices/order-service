package com.example.order_service.Clients;

import com.example.order_service.dto.CartResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "CART-SERVICE")
public interface CartClient {

    @GetMapping("/api/cart")
    CartResponseDTO getCart(
            @RequestParam("userId") Long userId
    );

    @DeleteMapping("/api/cart/clear")
    void clearCart(
            @RequestParam("userId") Long userId
    );
}