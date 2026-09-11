package com.microservices.OrderService.dto;

import com.microservices.OrderService.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long id;
    private Long userId;
    private String productName;
    private Integer quantity;
    private BigDecimal totalPrice;
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;
}
