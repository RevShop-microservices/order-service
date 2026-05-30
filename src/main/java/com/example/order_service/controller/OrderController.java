package com.example.order_service.controller;

import com.example.order_service.dto.*;
import com.example.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Controller", description = "APIs for order management")
public class OrderController {

    @Autowired
    private OrderService service;

    @PostMapping("/checkout")
    @Operation(summary = "Place order from cart")
    public ResponseEntity<OrderResponseDTO> placeOrder(@RequestBody PlaceOrderRequestDTO dto) {
        return ResponseEntity.ok(service.placeOrder(dto.getUserId(), dto.getAddressId(), dto.getCouponCode()));
    }

    @PostMapping("/buy-now")
    @Operation(summary = "Buy product directly")
    public ResponseEntity<OrderResponseDTO> buyNow(@RequestBody BuyNowRequestDTO dto) {
        return ResponseEntity.ok(service.buyNow(dto.getUserId(), dto.getProductId(), dto.getQuantity(), dto.getAddressId(), dto.getCouponCode()));
    }

    @GetMapping("/user")
    @Operation(summary = "Get user order history")
    public ResponseEntity<List<OrderResponseDTO>> getOrders(@RequestParam Long userId) {
        return ResponseEntity.ok(service.getOrdersByUser(userId));
    }

    @GetMapping("/check-purchase")
    @Operation(summary = "Check if user has purchased the product")
    public ResponseEntity<Boolean> checkPurchase(@RequestParam String productId, @RequestParam Long userId) {
        return ResponseEntity.ok(service.hasPurchasedProduct(productId, userId));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details")
    public ResponseEntity<OrderResponseDTO> getOrderDetails(@PathVariable Long orderId) {
        return ResponseEntity.ok(service.getOrderDetails(orderId));
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order")
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(service.cancelOrder(orderId));
    }

    @PutMapping("/{orderId}/cancel-item")
    @Operation(summary = "Cancel specific item in order")
    public ResponseEntity<OrderResponseDTO> cancelOrderItem(@PathVariable Long orderId, @RequestParam String productId) {
        return ResponseEntity.ok(service.cancelOrderItem(orderId, productId));
    }

    @PostMapping("/{orderId}/return")
    @Operation(summary = "Request return for a delivered order")
    public ResponseEntity<OrderResponseDTO> requestReturn(@PathVariable Long orderId, @RequestParam Long userId) {
        return ResponseEntity.ok(service.requestReturn(orderId, userId));
    }

    @PostMapping("/{orderId}/process-return")
    @Operation(summary = "Approve or deny a return request (Seller/Admin)")
    public ResponseEntity<OrderResponseDTO> processReturn(@PathVariable Long orderId, @RequestParam boolean approve) {
        return ResponseEntity.ok(service.processReturn(orderId, approve));
    }
}