package com.example.order_service.controller;

import com.example.order_service.models.Coupon;
import com.example.order_service.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "Coupon Controller", description = "APIs for coupon management")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @PostMapping
    @Operation(summary = "Create a new coupon")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(couponService.createCoupon(coupon));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate a coupon code")
    public ResponseEntity<Coupon> validateCoupon(@RequestParam String code) {
        return ResponseEntity.ok(couponService.validateCoupon(code));
    }

    @GetMapping
    @Operation(summary = "Get all coupons")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a coupon")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }
}
