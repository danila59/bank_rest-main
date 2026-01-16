package com.example.bankcards.service;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.UserOperationException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ValidationUtil validationUtil;

    @Transactional
    public User registerUser(AuthDTO.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserOperationException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserOperationException("Email already exists");
        }

        if (!validationUtil.isValidUsername(request.getUsername())) {
            throw new UserOperationException("Invalid username format");
        }

        if (!validationUtil.isValidName(request.getFirstName()) ||
                !validationUtil.isValidName(request.getLastName())) {
            throw new UserOperationException("Invalid name format");
        }

        if (!validationUtil.isValidEmail(request.getEmail())) {
            throw new UserOperationException("Invalid email");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findActiveById(id)
                .orElseThrow(() -> new UserOperationException("User not found with id: " + id, HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User user = getUserById(id);

        user.setFirstName(updatedUser.getFirstName());
        user.setLastName(updatedUser.getLastName());
        user.setRole(updatedUser.getRole());
        user.setEmail(updatedUser.getEmail());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Transactional
    public void disableUser(Long id) {
        User user = getUserById(id);
        user.setEnabled(false);
        userRepository.save(user);
        log.info("User disabled: {}", id);
    }

    @Transactional
    public void enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserOperationException("User not found with id: " + id, HttpStatus.NOT_FOUND));
        user.setEnabled(true);
        userRepository.save(user);
        log.info("User enabled: {}", id);
    }

    @Transactional(readOnly = true)
    public User getUserByUsername (String username){
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserOperationException("User not found by Username " + username));
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
