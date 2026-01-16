package com.example.bankcards.dto;

import com.example.bankcards.entity.enums.CardStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CardDTO {

    @Data
    @Builder
    public static class CreateRequest {

        @Schema(description = "Номер карты (16 цифр)", example = "4532733309169843")
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
        private String cardNumber;

        @Schema(description = "Имя владельца карты", example = "IVAN IVANOV")
        @NotBlank(message = "Owner name is required")
        private String ownerName;

        @Schema(description = "Срок действия (MM/yy)", example = "12/27")
        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        @JsonFormat(pattern = "MM/yy")
        private LocalDate expiryDate;

        @Schema(description = "CVV код (3 цифры)", example = "123")
        @NotBlank(message = "CVV is required")
        @Pattern(regexp = "\\d{3}", message = "CVV must be 3 digits")
        private String cvv;

        @Schema(description = "ID пользователя-владельца", example = "1")
        @NotNull(message = "User ID is required")
        private Long userId;

        @Schema(description = "Начальный баланс", example = "1000.00")
        private BigDecimal initialBalance;
    }

    @Data
    @Builder
    public static class UpdateStatusRequest {
        @NotNull(message = "Status is required")
        private CardStatus status;

        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String maskedNumber;
        private String ownerName;
        private String expiryDate;
        private CardStatus status;
        private BigDecimal balance;
        private boolean expired;
        private LocalDateTime createdAt;
        private Long userId;
        private String username;
    }

    @Data
    public static class FilterRequest {
        private CardStatus status;
        private String cardNumberLastFour;
        private String ownerName;
        private Integer page = 0;
        private Integer size = 10;
        private String sortBy = "createdAt";
        private String sortDirection = "DESC";
    }
}
