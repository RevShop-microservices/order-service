package com.example.order_service.service;

import com.example.order_service.Clients.CartClient;
import com.example.order_service.Clients.UserClient;
import com.example.order_service.CustomExceptions.*;
import com.example.order_service.dto.*;
import com.example.order_service.models.*;
import com.example.order_service.repository.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartClient cartClient;
    private final ProductRepository productRepository;

    @Autowired(required = false)
    private CouponService couponService;

    @Autowired(required = false)
    private PdfInvoiceService pdfInvoiceService;

    @Autowired(required = false)
    private UserClient userClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderResponseDTO mapToDTO(Order order) {
        List<OrderItem> items = Optional.ofNullable(order.getItems()).orElse(Collections.emptyList());
        List<OrderItemDTO> itemDTOs = items.stream()
                .map(item -> OrderItemDTO.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .totalPrice(item.getPrice() * item.getQuantity())
                        .cancelled(item.isCancelled())
                        .build()).toList();

        return OrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus() != null ? order.getStatus().name() : "UNKNOWN")
                .orderDate(order.getOrderDate())
                .addressId(order.getAddressId())
                .items(itemDTOs)
                .build();
    }

    private double calculateDiscount(Coupon coupon, double subtotal) {
        if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
            return Math.min(subtotal, subtotal * coupon.getDiscountValue() / 100.0);
        } else {
            return Math.min(subtotal, coupon.getDiscountValue());
        }
    }

    @Transactional
    public OrderResponseDTO placeOrder(Long userId, Long addressId, String couponCode) {
        log.info("Placing order for user {}", userId);
        CartResponseDTO cart = cartClient.getCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        double subtotal = cart.getTotalPrice();
        double discount = 0.0;
        Coupon coupon = null;

        if (couponCode != null && !couponCode.isBlank() && couponService != null) {
            coupon = couponService.validateCoupon(couponCode);
            discount = calculateDiscount(coupon, subtotal);
        }

        double totalAmount = subtotal - discount;

        Order order = Order.builder()
                .userId(userId)
                .subtotal(subtotal)
                .discount(discount)
                .totalAmount(totalAmount)
                .status(OrderStatus.PLACED)
                .orderDate(LocalDateTime.now())
                .addressId(addressId)
                .build();

        Order savedOrder = orderRepository.save(order);
        List<OrderItem> savedItems = new ArrayList<>();

        for (CartItemDTO item : cart.getItems()) {
            Products product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found: " + item.getProductId()));
            if (item.getQuantity() > product.getStock()) {
                throw new QuantityExceedStockException("Selected quantity exceeds stock for: " + product.getName());
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .sellerId(product.getSellerId())
                    .city(product.getCity())
                    .order(savedOrder)
                    .build();
            savedItems.add(orderItemRepository.save(orderItem));
        }

        savedOrder.setItems(savedItems);
        cartClient.clearCart(userId);

        if (coupon != null) couponService.incrementUsage(coupon);

        log.info("Order placed successfully: {}", savedOrder.getOrderId());

        // Send notification with invoice
        sendOrderPlacedNotification(userId, savedOrder);

        return mapToDTO(savedOrder);
    }

    @Transactional
    public OrderResponseDTO buyNow(Long userId, String productId, int quantity, Long addressId, String couponCode) {
        log.info("Buy Now triggered for user {} product {}", userId, productId);
        if (quantity <= 0) throw new InvalidRequestException("Quantity must be greater than 0");

        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        if (product.getStock() <= 0) throw new InvalidRequestException("Product is out of stock");
        if (quantity > product.getStock()) throw new InvalidRequestException("Not enough stock available");

        double subtotal = product.getPrice() * quantity;
        double discount = 0.0;
        Coupon coupon = null;

        if (couponCode != null && !couponCode.isBlank() && couponService != null) {
            coupon = couponService.validateCoupon(couponCode);
            discount = calculateDiscount(coupon, subtotal);
        }

        Order order = Order.builder()
                .userId(userId)
                .subtotal(subtotal)
                .discount(discount)
                .totalAmount(subtotal - discount)
                .status(OrderStatus.PLACED)
                .orderDate(LocalDateTime.now())
                .addressId(addressId)
                .build();

        Order savedOrder = orderRepository.save(order);
        OrderItem item = OrderItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .quantity(quantity)
                .price(product.getPrice())
                .sellerId(product.getSellerId())
                .city(product.getCity())
                .order(savedOrder)
                .build();

        OrderItem savedItem = orderItemRepository.save(item);
        savedOrder.setItems(List.of(savedItem));
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        if (coupon != null) couponService.incrementUsage(coupon);

        log.info("Buy Now order placed: {}", savedOrder.getOrderId());

        // Send notification with invoice
        sendOrderPlacedNotification(userId, savedOrder);

        return mapToDTO(savedOrder);
    }

    @Transactional
    public List<OrderResponseDTO> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public Order getOrderEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    @Transactional
    public OrderResponseDTO getOrderDetails(Long orderId) {
        Order order = getOrderEntity(orderId);
        return mapToDTO(order);
    }

    public boolean hasPurchasedProduct(String productId, Long userId) {
        return orderItemRepository.existsByProductIdAndOrderUserId(productId, userId);
    }

    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderCancelAfterShippingEXception("Cannot cancel after shipping");
        }
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
        for (OrderItem item : items) {
            if (!item.isCancelled()) {
                Products product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new ProductNotFoundException("Product not found"));
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
                item.setCancelled(true);
                orderItemRepository.save(item);
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // Notify user about cancellation
        try {
            if (userClient != null) {
                UserDTO user = userClient.getUserById(order.getUserId());
                rabbitTemplate.convertAndSend("order-exchange", "order.notification", NotificationRequest.builder()
                        .userId(order.getUserId())
                        .userEmail(user.getEmail())
                        .subject("Order Cancelled")
                        .message("Your order #" + savedOrder.getOrderId() + " has been cancelled.")
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to send cancellation notification", e);
        }

        return mapToDTO(savedOrder);
    }

    @Transactional
    public OrderResponseDTO cancelOrderItem(Long orderId, String productId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderCancelAfterShippingEXception("Cannot cancel after shipping");
        }
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
        boolean found = false;
        double refundAmount = 0.0;
        for (OrderItem item : items) {
            if (item.getProductId().equals(productId)) {
                if (item.isCancelled()) throw new InvalidRequestException("Item already cancelled");
                item.setCancelled(true);
                orderItemRepository.save(item);
                refundAmount += item.getPrice() * item.getQuantity();
                found = true;
                Products product = productRepository.findById(productId)
                        .orElseThrow(() -> new ProductNotFoundException("Product not found"));
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }
        if (!found) throw new CartItemNotFoundException("Item not found in order");
        order.setTotalAmount(order.getTotalAmount() - refundAmount);
        if (items.stream().allMatch(OrderItem::isCancelled)) order.setStatus(OrderStatus.CANCELLED);
        return mapToDTO(orderRepository.save(order));
    }

    @Transactional
    public OrderResponseDTO requestReturn(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) throw new InvalidRequestException("Unauthorized return request");
        if (order.getStatus() != OrderStatus.DELIVERED) throw new InvalidRequestException("Only delivered orders can be returned");
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        Order saved = orderRepository.save(order);

        // Notify seller(s) of the return request
        try {
            if (userClient != null) {
                List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
                for (OrderItem item : items) {
                    if (item.getSellerId() != null) {
                        try {
                            UserDTO seller = userClient.getUserById(item.getSellerId());
                            rabbitTemplate.convertAndSend("order-exchange", "order.notification", NotificationRequest.builder()
                                    .userId(seller.getId())
                                    .userEmail(seller.getEmail())
                                    .subject("Return Request Received")
                                    .message("A return has been requested for order #" + orderId + " (Product: "
                                            + item.getProductName() + ").")
                                    .build());
                        } catch (Exception e) {
                            log.error("Failed to notify seller {} about return request", item.getSellerId(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to notify seller about return request", e);
        }

        return mapToDTO(saved);
    }

    @Transactional
    public OrderResponseDTO processReturn(Long orderId, boolean approve) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) throw new InvalidRequestException("Order is not in return-requested state");
        if (approve) {
            order.setStatus(OrderStatus.REFUNDED);
            List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
            for (OrderItem item : items) {
                if (!item.isCancelled()) {
                    productRepository.findById(item.getProductId()).ifPresent(product -> {
                        product.setStock(product.getStock() + item.getQuantity());
                        productRepository.save(product);
                    });
                }
            }
        } else {
            order.setStatus(OrderStatus.RETURN_DENIED);
        }
        Order saved = orderRepository.save(order);

        // Notify user about return decision
        try {
            if (userClient != null) {
                UserDTO user = userClient.getUserById(order.getUserId());
                rabbitTemplate.convertAndSend("order-exchange", "order.notification", NotificationRequest.builder()
                        .userId(user.getId())
                        .userEmail(user.getEmail())
                        .subject(approve ? "Return Approved" : "Return Denied")
                        .message(approve
                                ? "Your return request for order #" + orderId
                                        + " has been approved. A refund will be processed shortly."
                                : "Your return request for order #" + orderId + " has been denied.")
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to send return notification", e);
        }

        return mapToDTO(saved);
    }

    // ── Private helper for invoice notification ──
    private void sendOrderPlacedNotification(Long userId, Order savedOrder) {
        try {
            if (userClient != null) {
                UserDTO user = userClient.getUserById(userId);
                if (user != null && pdfInvoiceService != null) {
                    byte[] invoiceBytes = pdfInvoiceService.generateInvoicePdf(savedOrder);
                    String base64Invoice = Base64.getEncoder().encodeToString(invoiceBytes);
                    rabbitTemplate.convertAndSend("order-exchange", "order.notification", NotificationRequest.builder()
                            .userId(userId)
                            .userEmail(user.getEmail())
                            .subject("Order Placed Successfully")
                            .message("Your order #" + savedOrder.getOrderId() + " has been placed successfully.")
                            .attachmentBase64(base64Invoice)
                            .attachmentFilename("invoice-" + savedOrder.getOrderId() + ".pdf")
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send order notification", e);
        }
    }
}