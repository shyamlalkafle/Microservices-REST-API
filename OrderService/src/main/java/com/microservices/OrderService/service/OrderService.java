package com.microservices.OrderService.service;

import com.microservices.OrderService.dto.OrderRequest;
import com.microservices.OrderService.dto.OrderResponse;
import com.microservices.OrderService.entity.Order;

import java.util.List;

public interface OrderService {

    List<OrderResponse> getAllOrders();

    OrderResponse getOrderById(Long id);

    OrderResponse createOrder(OrderRequest order);

    void deleteOrder(Long id);

    List<OrderResponse> getOrderByUserId(Long id);
}
