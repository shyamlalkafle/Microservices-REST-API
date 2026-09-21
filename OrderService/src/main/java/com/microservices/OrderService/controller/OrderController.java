package com.microservices.OrderService.controller;

import com.microservices.OrderService.dto.OrderRequest;
import com.microservices.OrderService.dto.OrderResponse;
import com.microservices.OrderService.entity.Order;
import com.microservices.OrderService.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.status(HttpStatus.OK).body(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.status(HttpStatus.OK).body(order);
    }

    /**
     * Create a new order
     * Practice: Secured with ADMIN role, supports API versioning and idempotency
     * 
     * @param request Validated order request
     * @param idempotencyKey Optional header to prevent duplicate requests
     * @return Created order response
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        OrderResponse savedOrder = orderService.createOrder(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);
    }

    /**
     * Test endpoint to demonstrate @Transactional rollback
     * Practice 9: This endpoint will fail intentionally to prove rollback works
     * 
     * Call this endpoint and then check database - order should NOT exist
     * This proves that @Transactional rolled back the entire transaction
     */
    @PostMapping("/test-rollback")
    public ResponseEntity<OrderResponse> createOrderWithRollback(@Valid @RequestBody OrderRequest request) {
        OrderResponse savedOrder = ((com.microservices.OrderService.service.OrderServiceImplementation) orderService)
                .createOrderWithRollbackTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Order with order id "+id+" deleted successfully");
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<OrderResponse>> getOrderByUserId(Long id){
        List<OrderResponse> orderList = orderService.getOrderByUserId(id);
        return ResponseEntity.status(HttpStatus.OK).body(orderList);
    }
}
