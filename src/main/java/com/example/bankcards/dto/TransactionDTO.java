package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionDTO {

    @Data
    @Schema(description = "Запрос на перевод между картами")
    public static class TransferRequest {
        @Schema(
                description = "Номер карты отправителя (16 цифр)",
                example = "4149471805568597",
                pattern = "\\d{16}",
                minLength = 16,
                maxLength = 16
        )
        @NotBlank(message = "From card number is required")
        @Pattern(regexp = "\\d{16}", message = "From card number must be 16 digits")
        private String fromCardNumber;

        @Schema(
                description = "Номер карты получателя (16 цифр)",
                example = "4149476684550065",
                pattern = "\\d{16}",
                minLength = 16,
                maxLength = 16
        )
        @NotBlank(message = "To card number is required")
        @Pattern(regexp = "\\d{16}", message = "To card number must be 16 digits")
        private String toCardNumber;

        @Schema(
                description = "Сумма перевода (0.01 - 1,000,000)",
                example = "500.50",
                minimum = "0.01",
                maximum = "1000000"
        )
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @DecimalMax(value = "1000000", message = "Amount must be less than 1,000,000")
        private BigDecimal amount;

        @Schema(
                description = "Описание перевода",
                example = "Перевод между моими картами",
                maxLength = 255
        )
        @Size(max = 255, message = "Description must be less than 255 characters")
        private String description;

        @Schema(
                description = "CVV код карты отправителя (3 цифры)",
                example = "244",
                pattern = "\\d{3}",
                minLength = 3,
                maxLength = 3
        )
        @NotBlank(message = "CVV is required for verification")
        @Pattern(regexp = "\\d{3}", message = "CVV must be 3 digits")
        private String cvv;
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private String transactionId;
        private BigDecimal amount;
        private String currency;
        private String type;
        private String status;
        private String description;
        private String fromCardMasked;
        private String toCardMasked;
        private LocalDateTime transactionDate;
        private LocalDateTime createdAt;
    }
}