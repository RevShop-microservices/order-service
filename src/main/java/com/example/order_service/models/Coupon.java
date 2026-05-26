package com.example.order_service.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private String discountType; // PERCENTAGE or FLAT

    private Double discountValue;

    private Integer usageLimit;

    @Builder.Default
    private Integer usedCount = 0;

    private LocalDateTime expiryDate;

    @Builder.Default
    private boolean active = true;
}
