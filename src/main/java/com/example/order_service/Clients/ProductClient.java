package com.example.order_service.Clients;

import com.example.order_service.dto.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponseDTO getProductById(@PathVariable String id);

    @PutMapping("/api/products/{id}/reduce-stock")
    void reduceStock(@PathVariable String id,
            @RequestParam int quantity
    );

    @PutMapping("/{id}/increase-stock")
    public ResponseEntity<Void> increaseStock(@PathVariable String id,
            @RequestParam int quantity);
}