package com.microservices.OrderService.service;

import com.microservices.OrderService.client.PaymentServiceClient;
import com.microservices.OrderService.client.UserServiceClient;
import com.microservices.OrderService.dto.OrderRequest;
import com.microservices.OrderService.dto.OrderResponse;
import com.microservices.OrderService.entity.Order;
import com.microservices.OrderService.enums.OrderStatus;
import com.microservices.OrderService.exception.OrderNotFoundException;
import com.microservices.OrderService.exception.UserNotFoundException;
import com.microservices.OrderService.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImplementation implements OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    public OrderServiceImplementation(OrderRepository orderRepository, UserServiceClient userServiceClient, PaymentServiceClient paymentServiceClient) {
        this.orderRepository = orderRepository;
        this.userServiceClient = userServiceClient;
        this.paymentServiceClient = paymentServiceClient;
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository
                .findById(id)
                .orElseThrow(()-> new OrderNotFoundException("Order with id " + id + " not found"));
        return mapToResponse(order);
    }

    @Override
    public OrderResponse createOrder(OrderRequest requestOrder) {
        if (!userServiceClient.userExists(requestOrder.getUserId())) {
            throw new UserNotFoundException("User with id " + requestOrder.getUserId() + " does not exist");
        }
        Order order = mapToEntity(requestOrder);
        Order savedOrder = orderRepository.save(order);

        boolean paymentSuccess = paymentServiceClient.processPayment(
                savedOrder.getId(), savedOrder.getUserId(), savedOrder.getAmount()
        );

        savedOrder.setOrderStatus(paymentSuccess ? OrderStatus.CONFIRMED : OrderStatus.FAILED);
        return mapToResponse(orderRepository.save(savedOrder));
    }

    @Override
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException("Order with id " + id + " not found");
        }
        orderRepository.deleteById(id);
    }

    @Override
    public List<OrderResponse> getOrderByUserId(Long id){
        List<Order> orders = orderRepository.findByUserId(id);
        return orders.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private OrderResponse mapToResponse(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getProductName(),
                order.getQuantity(),
                order.getAmount(),
                order.getOrderStatus(),
                order.getOrderDate()
        );
    }

    private Order mapToEntity(OrderRequest orderRequest) {
        if (orderRequest == null) {
            return null;
        }
        Order order = new Order();

        order.setUserId(orderRequest.getUserId());
        order.setProductName(orderRequest.getProductName());
        order.setQuantity(orderRequest.getQuantity());
        order.setAmount(orderRequest.getAmount());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        return order;
    }
}
