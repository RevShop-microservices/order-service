package com.example.order_service.controller;

import com.example.order_service.models.Payout;
import com.example.order_service.service.PayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payouts")
@Tag(name = "Payout Controller", description = "APIs for payout and commission management")
public class PayoutController {

    @Autowired
    private PayoutService payoutService;

    @PostMapping("/generate")
    @Operation(summary = "Generate payout for a seller")
    public ResponseEntity<Payout> generatePayout(@RequestParam Long sellerId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return ResponseEntity.ok(payoutService.generatePayout(sellerId, monthStart, now));
    }

    @PutMapping("/{payoutId}/mark-paid")
    @Operation(summary = "Mark a payout as paid (Admin)")
    public ResponseEntity<Payout> markAsPaid(@PathVariable Long payoutId) {
        return ResponseEntity.ok(payoutService.markAsPaid(payoutId));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get all pending payouts")
    public ResponseEntity<List<Payout>> getPendingPayouts() {
        return ResponseEntity.ok(payoutService.getPendingPayouts());
    }

    @GetMapping("/seller")
    @Operation(summary = "Get payouts for a specific seller")
    public ResponseEntity<List<Payout>> getSellerPayouts(@RequestParam Long sellerId) {
        return ResponseEntity.ok(payoutService.getPayoutsBySeller(sellerId));
    }

    @GetMapping
    @Operation(summary = "Get all payouts (Admin)")
    public ResponseEntity<List<Payout>> getAllPayouts() {
        return ResponseEntity.ok(payoutService.getAllPayouts());
    }

    @GetMapping("/commission-rate")
    @Operation(summary = "Get dynamic commission rate for a seller")
    public ResponseEntity<Double> getCommissionRate(@RequestParam Long sellerId) {
        return ResponseEntity.ok(payoutService.calculateCommissionRate(sellerId));
    }
}
