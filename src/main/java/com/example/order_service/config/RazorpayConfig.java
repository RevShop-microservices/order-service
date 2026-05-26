package com.example.order_service.config;

import com.razorpay.RazorpayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key}")
    private String razorpayKey;

    @Value("${razorpay.secret}")
    private String razorpaySecret;

    @Bean
    public RazorpayClient razorpayClient() {
        try {
            return new RazorpayClient(razorpayKey, razorpaySecret);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Razorpay client", e);
        }
    }
}
