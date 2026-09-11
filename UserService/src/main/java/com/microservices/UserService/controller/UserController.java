package com.microservices.UserService.controller;

import com.microservices.UserService.dto.AmountRequest;
import com.microservices.UserService.dto.UserRequestDto;
import com.microservices.UserService.dto.UserResponse;
import com.microservices.UserService.entity.User;
import com.microservices.UserService.service.UserService;
import com.microservices.UserService.service.UserServiceImplementation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserServiceImplementation userServiceImplementation;

    public UserController(UserService userService, UserServiceImplementation userServiceImplementation) {
        this.userService = userService;
        this.userServiceImplementation = userServiceImplementation;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUser().stream()
                .map(userServiceImplementation::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        UserResponse response = userServiceImplementation.mapToResponse(user);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> existsById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.existById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequestDto user) {
        User savedUser = userService.createUser(user);
        UserResponse response = userServiceImplementation.mapToResponse(savedUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequestDto user) {
        User updatedUser = userService.updateUser(id, user);
        UserResponse response = userServiceImplementation.mapToResponse(updatedUser);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User with user id "+id+" deleted successfully");
    }

    @PatchMapping("/{id}/deduct")
    public ResponseEntity<Boolean> deductBalance(@PathVariable Long id, @RequestParam BigDecimal amount) {
        boolean success = userService.deductBalanceIfSufficient(id, amount);
        return ResponseEntity.ok(success);
    }

    @PostMapping("/{id}/balance/add")
    public ResponseEntity<User> addBalance(
            @PathVariable Long id,
            @RequestBody AmountRequest request) {
        User updated = userService.addToBalance(id, request.getAmount());
        return ResponseEntity.ok(updated);
    }
}
