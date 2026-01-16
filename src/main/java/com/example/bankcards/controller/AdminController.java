package com.example.bankcards.controller;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "API для административного управления пользователями")
@SecurityRequirement(name = "bearerAuth")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final ResponseUtil responseUtil;
    private final UserMapper userMapper;

    @GetMapping
    @Operation(summary = "Получить всех пользователей", description = "Получить список всех пользователей системы")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDTO> response = users.stream().map(userMapper::toResponse).toList();
        return responseUtil.successResponse("Users retrieved successfully", response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Получить пользователя по ID", description = "Получить информацию о пользователе")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        UserDTO response = userMapper.toResponse(user);
        return responseUtil.successResponse("User retrieved successfully", response);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Обновить пользователя", description = "Обновить информацию о пользователе")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody User updatedUser) {
        User user = userService.updateUser(userId, updatedUser);
        UserDTO response = userMapper.toResponse(user);
        return responseUtil.successResponse("User updated successfully", response);
    }

    @PostMapping("/{userId}/disable")
    @Operation(summary = "Отключить пользователя", description = "Отключить аккаунт пользователя")
    public ResponseEntity<?> disableUser(@PathVariable Long userId) {
        userService.disableUser(userId);
        return responseUtil.successResponse("User disabled successfully", null);
    }

    @PostMapping("/{userId}/enable")
    @Operation(summary = "Включить пользователя", description = "Включить аккаунт пользователя")
    public ResponseEntity<?> enableUser(@PathVariable Long userId) {
        userService.enableUser(userId);
        return responseUtil.successResponse("User enabled successfully", null);
    }

    @GetMapping("/check-username/{username}")
    @Operation(summary = "Проверить имя пользователя", description = "Проверить существует ли имя пользователя")
    public ResponseEntity<?> checkUsername(@PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        return responseUtil.successResponse("Username check completed", exists);
    }

    @GetMapping("/check-email/{email}")
    @Operation(summary = "Проверить email", description = "Проверить существует ли email")
    public ResponseEntity<?> checkEmail(@PathVariable String email) {
        boolean exists = userService.existsByEmail(email);
        return responseUtil.successResponse("Email check completed", exists);
    }
}