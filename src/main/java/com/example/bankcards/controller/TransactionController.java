package com.example.bankcards.controller;


import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.mapper.TransactionMapper;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "API для управления транзакциями")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthService authService;
    private final ResponseUtil responseUtil;
    private final CardService cardService;
    private final TransactionMapper transactionMapper;

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Перевод между картами", description = "Перевод средств между своими картами")
    public ResponseEntity<?> transferBetweenCards(@Valid @RequestBody TransactionDTO.TransferRequest request) {
        Long userId = authService.getCurrentUserId();
        Transaction transaction = transactionService.transferBetweenOwnCards(request, userId);

        TransactionDTO.Response response = transactionMapper.toResponse(transaction);
        return responseUtil.createdResponse("Transfer completed successfully", response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Получить транзакции пользователя", description = "Получить историю транзакций текущего пользователя")
    public ResponseEntity<?> getUserTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = authService.getCurrentUserId();
        Page<Transaction> transactions = transactionService.getUserTransactions(userId, page, size);

        Page<TransactionDTO.Response> responsePage = transactions.map(transactionMapper::toResponse);
        return responseUtil.successResponse(
                "Transactions retrieved successfully",
                responseUtil.paginatedResponse(
                        responsePage.getContent(),
                        page,
                        size,
                        responsePage.getTotalElements(),
                        responsePage.getTotalPages()
                )
        );
    }

    @GetMapping("/card/{cardId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Получить транзакции карты", description = "Получить историю транзакций конкретной карты")
    public ResponseEntity<?> getCardTransactions(
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = authService.getCurrentUserId();
        cardService.getCardByIdAndOwnerId(cardId, userId);

        Page<Transaction> transactions = transactionService.getCardTransactions(cardId, page, size);

        Page<TransactionDTO.Response> responsePage = transactions.map(transactionMapper::toResponse);
        return responseUtil.successResponse(
                "Card transactions retrieved successfully",
                responseUtil.paginatedResponse(
                        responsePage.getContent(),
                        page,
                        size,
                        responsePage.getTotalElements(),
                        responsePage.getTotalPages()
                )
        );
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Получить транзакцию по ID", description = "Получить детали конкретной транзакции")
    public ResponseEntity<?> getTransaction(@PathVariable String transactionId) {
        Transaction transaction = transactionService.getTransactionById(transactionId);

        TransactionDTO.Response response = transactionMapper.toResponse(transaction);
        return responseUtil.successResponse("Transaction retrieved successfully", response);
    }

    @GetMapping("/card/{cardId}/daily-limit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Проверить дневной лимит", description = "Проверить использованный дневной лимит для карты")
    public ResponseEntity<?> checkDailyLimit(@PathVariable Long cardId) {
        Long userId = authService.getCurrentUserId();
        cardService.getCardByIdAndOwnerId(cardId, userId);

        BigDecimal dailyTotal = transactionService.getTotalTransferredAmount(cardId, 1);
        BigDecimal dailyLimit = new BigDecimal("5000");
        BigDecimal remaining = dailyLimit.subtract(dailyTotal);

        Map<String, Object> response = new HashMap<>();
        response.put("dailyTotal", dailyTotal);
        response.put("dailyLimit", dailyLimit);
        response.put("remaining", remaining);
        response.put("limitExceeded", dailyTotal.compareTo(dailyLimit) >= 0);

        return responseUtil.successResponse("Daily limit checked successfully", response);
    }
}