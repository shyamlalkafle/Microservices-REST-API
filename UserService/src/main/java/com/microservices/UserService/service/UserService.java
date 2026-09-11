package com.microservices.UserService.service;

import com.microservices.UserService.dto.UserRequestDto;
import com.microservices.UserService.dto.UserResponse;
import com.microservices.UserService.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface UserService {

    List<User> getAllUser();

    User getUserById(Long id);

    boolean existById(Long id);

    User createUser(UserRequestDto user);

    User updateUser(Long id, UserRequestDto updatedUser);

    void deleteUser(Long id);

    public UserResponse mapToResponse(User user);

    boolean deductBalanceIfSufficient(Long id, BigDecimal amount);

    User addToBalance(Long userId, BigDecimal amount);
}
