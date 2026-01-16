package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthDTO {

    @Data
    public static class LoginRequest {
        @Schema(description = "Имя пользователя", example = "testuser")
        @NotBlank(message = "Username is required")
        private String username;

        @Schema(description = "Пароль", example = "password")
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @Schema(description = "Запрос на регистрацию нового пользователя")
    public static class RegisterRequest {
        @Schema(
                description = "Имя пользователя (от 3 до 50 символов)",
                example = "danila",
                minLength = 3,
                maxLength = 50
        )
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @Schema(
                description = "Пароль (минимум 6 символов)",
                example = "Danila12345",
                minLength = 6
        )
        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @Schema(description = "Имя", example = "Danila")
        @NotBlank(message = "First name is required")
        private String firstName;

        @Schema(description = "Фамилия", example = "Testov")
        @NotBlank(message = "Last name is required")
        private String lastName;

        @Schema(
                description = "Email адрес",
                example = "danila@example.com",
                format = "email"
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        private String email;
    }

    @Data
    public static class JwtResponse {
        private String token;
        private String type = "Bearer";
        private Long id;
        private String username;
        private String email;
        private String role;

        public JwtResponse(String token, Long id, String username, String email, String role) {
            this.token = token;
            this.id = id;
            this.username = username;
            this.email = email;
            this.role = role;
        }
    }
}