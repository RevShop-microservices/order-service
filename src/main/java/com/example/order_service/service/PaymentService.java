package com.example.order_service.service;

import com.example.order_service.dto.ApiResponse;
import com.example.order_service.models.Payment;
import com.example.order_service.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaymentService {

    private RazorpayClient razorpayClient;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${razorpay.key}")
    private String razorpayKey;

    @Value("${razorpay.secret}")
    private String razorpaySecret;

    @PostConstruct
    public void initRazorpay() {
        try {
            razorpayClient = new RazorpayClient(razorpayKey, razorpaySecret);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Razorpay: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> createOrder(String email, int amount) throws Exception {
        JSONObject options = new JSONObject();
        options.put("amount", amount * 100);
        options.put("currency", "INR");
        options.put("receipt", "txn_" + System.currentTimeMillis());

        Order order = razorpayClient.orders.create(options);

        Payment payment = Payment.builder()
                .email(email)
                .orderId(order.get("id"))
                .amount(amount)
                .status("PENDING")
                .build();

        paymentRepository.save(payment);

        return Map.of(
                "success", true,
                "orderId", order.get("id"),
                "amount", amount,
                "currency", "INR"
        );
    }

    public ApiResponse<String> verifyPayment(String orderId, String paymentId, String signature) throws Exception {

        String payload = orderId + "|" + paymentId;
        boolean isValid = Utils.verifySignature(payload, signature, razorpaySecret);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (isValid) {
            payment.setPaymentId(paymentId);
            payment.setSignature(signature);
            payment.setStatus("SUCCESS");
            paymentRepository.save(payment);
            return new ApiResponse<>(true, "Payment Successful", paymentId);
        }

        payment.setStatus("FAILED");
        paymentRepository.save(payment);
        return new ApiResponse<>(false, "Payment Verification Failed", null);
    }
}
