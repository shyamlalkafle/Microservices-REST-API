package com.microservices.OrderService.service;

import com.microservices.OrderService.client.PaymentServiceClient;
import com.microservices.OrderService.client.UserServiceClient;
import com.microservices.OrderService.dto.OrderItemRequest;
import com.microservices.OrderService.dto.OrderRequest;
import com.microservices.OrderService.dto.OrderResponse;
import com.microservices.OrderService.entity.Order;
import com.microservices.OrderService.entity.OrderItem;
import com.microservices.OrderService.enums.OrderStatus;
import com.microservices.OrderService.exception.InvalidOrderException;
import com.microservices.OrderService.exception.OrderNotFoundException;
import com.microservices.OrderService.exception.UserNotFoundException;
import com.microservices.OrderService.repository.OrderItemRepository;
import com.microservices.OrderService.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImplementation implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserServiceClient userServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final IdempotencyService idempotencyService;

    public OrderServiceImplementation(OrderRepository orderRepository, 
                                     OrderItemRepository orderItemRepository,
                                     UserServiceClient userServiceClient, 
                                     PaymentServiceClient paymentServiceClient,
                                     IdempotencyService idempotencyService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userServiceClient = userServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.idempotencyService = idempotencyService;
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

    /**
     * Create order with transactional support
     * Practice 9: @Transactional ensures Order + OrderItems are saved atomically
     * If any step fails, entire transaction rolls back
     */
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest requestOrder) {
        return createOrder(requestOrder, null);
    }
    
    /**
     * Create order with idempotency key support
     * Practice: Duplicate request protection with idempotency key
     * 
     * @param requestOrder Order details
     * @param idempotencyKey Optional unique key to prevent duplicate requests
     * @return Created order response
     * @throws DuplicateRequestException if idempotency key already used
     */
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest requestOrder, String idempotencyKey) {
        log.info("Creating order for user: {}, idempotencyKey: {}", 
                 requestOrder.getUserId(), idempotencyKey);
        
        // Step 0: Check for duplicate request using idempotency key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.checkDuplicateRequest(idempotencyKey);
        }
        
        // Step 1: Verify user exists
        if (!userServiceClient.userExists(requestOrder.getUserId())) {
            throw new UserNotFoundException("User with id " + requestOrder.getUserId() + " does not exist");
        }
        
        // Step 2: Create and save order
        Order order = mapToEntity(requestOrder);
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {}", savedOrder.getId());
        
        // Step 3: Process payment
        boolean paymentSuccess = paymentServiceClient.processPayment(
                savedOrder.getId(), savedOrder.getUserId(), savedOrder.getAmount()
        );
        
        savedOrder.setOrderStatus(paymentSuccess ? OrderStatus.CONFIRMED : OrderStatus.FAILED);
        
        // Step 4: Save order items if provided (transactional - will rollback if fails)
        if (requestOrder.getOrderItems() != null && !requestOrder.getOrderItems().isEmpty()) {
            log.info("Saving {} order items", requestOrder.getOrderItems().size());
            saveOrderItems(savedOrder, requestOrder.getOrderItems());
        }
        
        // Step 5: Save final order status
        Order finalOrder = orderRepository.save(savedOrder);
        log.info("Order {} completed with status: {}", finalOrder.getId(), finalOrder.getOrderStatus());
        
        // Step 6: Save idempotency record for successful order
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.saveIdempotencyRecord(
                idempotencyKey, 
                finalOrder.getId(), 
                finalOrder.getOrderStatus().toString()
            );
        }
        
        return mapToResponse(finalOrder);
    }
    
    /**
     * Save order items transactionally
     * If this method throws an exception, the entire transaction (including Order) rolls back
     */
    private void saveOrderItems(Order order, List<OrderItemRequest> itemRequests) {
        for (OrderItemRequest itemRequest : itemRequests) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductName(itemRequest.getProductName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(itemRequest.getUnitPrice());
            
            // Add to order (maintains bidirectional relationship)
            order.addOrderItem(orderItem);
        }
        
        // Items are saved via cascade when order is saved
        log.info("Order items added to order: {}", order.getOrderItems().size());
    }
    
    /**
     * Demonstration method: Force rollback to test @Transactional
     * This method intentionally throws an exception after saving order
     * to prove that the transaction rolls back
     */
    @Transactional
    public OrderResponse createOrderWithRollbackTest(OrderRequest requestOrder) {
        log.warn("ROLLBACK TEST: This transaction will intentionally fail");
        
        // Save order
        Order order = mapToEntity(requestOrder);
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {} - but will be rolled back", savedOrder.getId());
        
        // Save order items if provided
        if (requestOrder.getOrderItems() != null && !requestOrder.getOrderItems().isEmpty()) {
            saveOrderItems(savedOrder, requestOrder.getOrderItems());
            orderRepository.save(savedOrder); // Save items via cascade
            log.info("Order items saved - but will be rolled back");
        }
        
        // FORCE FAILURE - This will rollback the entire transaction
        throw new InvalidOrderException(
            "ROLLBACK TEST: Simulated failure - Order and OrderItems should NOT be saved in database"
        );
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
