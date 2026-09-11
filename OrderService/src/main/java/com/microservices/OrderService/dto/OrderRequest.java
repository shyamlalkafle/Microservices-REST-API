package com.microservices.OrderService.dto;

import com.microservices.OrderService.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {

    private Long userId;

    private String productName;

    private Integer quantity;

    private BigDecimal amount;


}
