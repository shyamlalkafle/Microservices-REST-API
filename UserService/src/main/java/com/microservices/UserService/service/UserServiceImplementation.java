package com.microservices.UserService.service;

import com.microservices.UserService.dto.UserRequestDto;
import com.microservices.UserService.dto.UserResponse;
import com.microservices.UserService.entity.User;
import com.microservices.UserService.exception.ResourceNotFoundException;
import com.microservices.UserService.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserServiceImplementation implements UserService{

    private final UserRepository userRepository;

    public UserServiceImplementation(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }

    public boolean existById(Long id) {
        return userRepository.existsById(id);
    }

    public User createUser(UserRequestDto user) {
        return userRepository.save(mapToEntity(user));
    }

    public User updateUser(Long id, UserRequestDto updatedUser) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setName(updatedUser.getName());
                    existingUser.setPhoneNo(updatedUser.getPhoneNo());
                    existingUser.setAddress(updatedUser.getAddress());
                    return userRepository.save(existingUser);
                })
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User with id " + id + " not found");
        }
        userRepository.deleteById(id);
    }

    public UserResponse mapToResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getPhoneNo(),
                user.getAddress(),
                user.getBalance()
        );
    }

    private User mapToEntity(UserRequestDto requestDto){
        if (requestDto == null) {
            return null;
        }
        User user = new User();
        user.setName(requestDto.getName());
        user.setPhoneNo(requestDto.getPhoneNo());
        user.setAddress(requestDto.getAddress());
        user.setBalance(BigDecimal.ZERO);
        return user;
    }

    @Transactional
    public boolean deductBalanceIfSufficient(Long userId, BigDecimal amount) {
        User user = getUserById(userId);

        if (user.getBalance().compareTo(amount) >= 0) {
            user.setBalance(user.getBalance().subtract(amount));
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Override
    public User addToBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        User user = getUserById(userId);
        user.setBalance(user.getBalance().add(amount));
        return userRepository.save(user);
    }
}
