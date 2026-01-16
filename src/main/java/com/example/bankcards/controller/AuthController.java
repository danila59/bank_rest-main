package com.example.bankcards.controller;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "API для аутентификации и регистрации")
public class AuthController {

    private final AuthService authService;
    private final ResponseUtil responseUtil;
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/login")
    @Operation(summary = "Вход в систему", description = "Аутентификация пользователя и получение JWT токена")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthDTO.LoginRequest loginRequest) {
        AuthDTO.JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return responseUtil.successResponse("User authenticated successfully", jwtResponse);
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация", description = "Регистрация нового пользователя")
    public ResponseEntity<?> registerUser(@Valid @RequestBody AuthDTO.RegisterRequest registerRequest) {
        AuthDTO.JwtResponse jwtResponse = authService.registerUser(registerRequest);
        return responseUtil.createdResponse("User registered successfully", jwtResponse);
    }

    @GetMapping("/me")
    @Operation(summary = "Текущий пользователь", description = "Получить информацию о текущем аутентифицированном пользователе")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return responseUtil.errorResponse(
                    HttpStatus.UNAUTHORIZED,
                    "User not authenticated",
                    "Please login first"
            );
        }

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        UserDTO response = userMapper.toResponse(user);

        return responseUtil.successResponse("Current user retrieved", response);
    }
}