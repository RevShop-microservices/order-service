package com.example.order_service.service;

import com.example.order_service.CustomExceptions.InvalidRequestException;
import com.example.order_service.models.Coupon;
import com.example.order_service.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    public Coupon createCoupon(Coupon coupon) {
        if (couponRepository.findByCode(coupon.getCode()).isPresent()) {
            throw new InvalidRequestException("Coupon code already exists");
        }
        return couponRepository.save(coupon);
    }

    public Coupon validateCoupon(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new InvalidRequestException("Invalid coupon code"));
        if (!coupon.isActive()) throw new InvalidRequestException("Coupon is inactive");
        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDateTime.now()))
            throw new InvalidRequestException("Coupon has expired");
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit())
            throw new InvalidRequestException("Coupon usage limit reached");
        return coupon;
    }

    public void incrementUsage(Coupon coupon) {
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }
}