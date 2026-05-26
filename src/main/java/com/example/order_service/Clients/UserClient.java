package com.example.order_service.Clients;

import com.example.order_service.dto.NotificationRequest;
import com.example.order_service.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "USER-SERVICE")
public interface UserClient {

    @PostMapping("/api/auth/notifications/send")
    void sendNotification(@RequestBody NotificationRequest request);

    @PostMapping("/api/auth/notifications/send-with-attachment")
    void sendNotificationWithAttachment(@RequestBody NotificationRequest request);

    @GetMapping("/api/auth/user/{userId}")
    UserDTO getUserById(@PathVariable Long userId);
}
