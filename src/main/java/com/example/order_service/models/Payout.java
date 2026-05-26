package com.example.order_service.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payouts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sellerId;
    private Double grossAmount;
    private Double commissionRate;
    private Double commissionAmount;
    private Double netPayout;

    @Builder.Default
    private String status = "PENDING";

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
