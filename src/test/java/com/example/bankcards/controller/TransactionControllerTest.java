package com.example.bankcards.controller;

import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.mapper.TransactionMapper;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.util.ResponseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AuthService authService;

    @Mock
    private ResponseUtil responseUtil;

    @Mock
    private CardService cardService;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionController transactionController;

    private ObjectMapper objectMapper;
    private TransactionDTO.TransferRequest transferRequest;
    private TransactionDTO.Response transactionResponse;
    private Transaction testTransaction;
    private User testUser;
    private Card testCard;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        objectMapper = new ObjectMapper();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        testCard = Card.builder()
                .id(1L)
                .cardNumber("1234567812345678")
                .maskedNumber("1234****5678")
                .ownerName("TEST USER")
                .balance(new BigDecimal("5000.00"))
                .status(CardStatus.ACTIVE)
                .owner(testUser)
                .build();

        testTransaction = Transaction.builder()
                .transactionId("TXN123456")
                .amount(new BigDecimal("100.00"))
                .description("Test transfer")
                .fromCard(testCard)
                .toCard(testCard)
                .transactionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        transferRequest = new TransactionDTO.TransferRequest();
        transferRequest.setFromCardNumber("1234567812345678");
        transferRequest.setToCardNumber("8765432187654321");
        transferRequest.setAmount(new BigDecimal("100.00"));
        transferRequest.setDescription("Test transfer");
        transferRequest.setCvv("123");

        transactionResponse = TransactionDTO.Response.builder()
                .transactionId("TXN123456")
                .amount(new BigDecimal("100.00"))
                .type("TRANSFER")
                .status("COMPLETED")
                .description("Test transfer")
                .fromCardMasked("1234****5678")
                .toCardMasked("8765****4321")
                .transactionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void transferBetweenCards_ShouldCompleteTransfer() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);
        when(transactionService.transferBetweenOwnCards(any(TransactionDTO.TransferRequest.class), eq(1L)))
                .thenReturn(testTransaction);
        when(transactionMapper.toResponse(testTransaction)).thenReturn(transactionResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "Transfer completed successfully",
                "data", transactionResponse
        );
        when(responseUtil.createdResponse(eq("Transfer completed successfully"), any(TransactionDTO.Response.class)))
                .thenReturn(ResponseEntity.status(201).body(responseBody));

        mockMvc.perform(post("/api/transactions/transfer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Transfer completed successfully"))
                .andExpect(jsonPath("$.data.transactionId").value("TXN123456"));

        verify(authService).getCurrentUserId();
        verify(transactionService).transferBetweenOwnCards(any(TransactionDTO.TransferRequest.class), eq(1L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void transferBetweenCards_WithInvalidAmount_ShouldReturnBadRequest() throws Exception {
        TransactionDTO.TransferRequest invalidRequest = new TransactionDTO.TransferRequest();
        invalidRequest.setFromCardNumber("1234567812345678");
        invalidRequest.setToCardNumber("8765432187654321");
        invalidRequest.setAmount(new BigDecimal("-100.00")); // Отрицательная сумма
        invalidRequest.setCvv("123");

        mockMvc.perform(post("/api/transactions/transfer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void transferBetweenCards_WithEmptyCVV_ShouldReturnBadRequest() throws Exception {

        TransactionDTO.TransferRequest invalidRequest = new TransactionDTO.TransferRequest();
        invalidRequest.setFromCardNumber("1234567812345678");
        invalidRequest.setToCardNumber("8765432187654321");
        invalidRequest.setAmount(new BigDecimal("100.00"));
        invalidRequest.setCvv("");

        mockMvc.perform(post("/api/transactions/transfer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserTransactions_ShouldReturnTransactions() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);

        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction),
                PageRequest.of(0, 20, Sort.Direction.DESC, "createdAt"), 1);

        when(transactionService.getUserTransactions(1L, 0, 20)).thenReturn(transactionPage);
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(transactionResponse);

        Map<String, Object> paginatedResponse = Map.of(
                "items", List.of(transactionResponse),
                "currentPage", 0,
                "totalItems", 1L,
                "totalPages", 1
        );

        Map<String, Object> responseBody = Map.of(
                "message", "Transactions retrieved successfully",
                "data", paginatedResponse
        );

        when(responseUtil.successResponse(eq("Transactions retrieved successfully"), any()))
                .thenReturn(ResponseEntity.ok(responseBody));

        when(responseUtil.paginatedResponse(anyList(), eq(0), eq(20), eq(1L), eq(1)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transactions retrieved successfully"))
                .andExpect(jsonPath("$.data.items[0].transactionId").value("TXN123456"));

        verify(authService).getCurrentUserId();
        verify(transactionService).getUserTransactions(1L, 0, 20);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserTransactions_WithCustomPagination_ShouldUseParameters() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(1L);

        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction),
                PageRequest.of(2, 50, Sort.Direction.ASC, "amount"), 1);

        when(transactionService.getUserTransactions(1L, 2, 50)).thenReturn(transactionPage);
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(transactionResponse);

        Map<String, Object> paginatedResponse = Map.of(
                "items", List.of(transactionResponse),
                "currentPage", 2,
                "totalItems", 1L,
                "totalPages", 1
        );

        Map<String, Object> responseBody = Map.of(
                "message", "Transactions retrieved successfully",
                "data", paginatedResponse
        );

        when(responseUtil.successResponse(eq("Transactions retrieved successfully"), any()))
                .thenReturn(ResponseEntity.ok(responseBody));

        when(responseUtil.paginatedResponse(anyList(), eq(2), eq(50), eq(1L), eq(1)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/transactions")
                        .param("page", "2")
                        .param("size", "50"))
                .andExpect(status().isOk());

        verify(transactionService).getUserTransactions(1L, 2, 50);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCardTransactions_ShouldReturnCardTransactions() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(1L);

        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction),
                PageRequest.of(0, 20, Sort.Direction.DESC, "createdAt"), 1);

        when(cardService.getCardByIdAndOwnerId(1L, 1L)).thenReturn(testCard);
        when(transactionService.getCardTransactions(1L, 0, 20)).thenReturn(transactionPage);
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(transactionResponse);

        Map<String, Object> paginatedResponse = Map.of(
                "items", List.of(transactionResponse),
                "currentPage", 0,
                "totalItems", 1L,
                "totalPages", 1
        );

        Map<String, Object> responseBody = Map.of(
                "message", "Card transactions retrieved successfully",
                "data", paginatedResponse
        );

        when(responseUtil.successResponse(eq("Card transactions retrieved successfully"), any()))
                .thenReturn(ResponseEntity.ok(responseBody));

        when(responseUtil.paginatedResponse(anyList(), eq(0), eq(20), eq(1L), eq(1)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/transactions/card/{cardId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card transactions retrieved successfully"));

        verify(authService).getCurrentUserId();
        verify(cardService).getCardByIdAndOwnerId(1L, 1L);
        verify(transactionService).getCardTransactions(1L, 0, 20);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getTransaction_ShouldReturnTransaction() throws Exception {

        when(transactionService.getTransactionById("TXN123456")).thenReturn(testTransaction);
        when(transactionMapper.toResponse(testTransaction)).thenReturn(transactionResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "Transaction retrieved successfully",
                "data", transactionResponse
        );
        when(responseUtil.successResponse(eq("Transaction retrieved successfully"), any(TransactionDTO.Response.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/transactions/{transactionId}", "TXN123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction retrieved successfully"))
                .andExpect(jsonPath("$.data.transactionId").value("TXN123456"));

        verify(transactionService).getTransactionById("TXN123456");
    }

    @Test
    @WithMockUser(roles = "USER")
    void checkDailyLimit_ShouldReturnLimitInfo() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);
        when(cardService.getCardByIdAndOwnerId(1L, 1L)).thenReturn(testCard);
        when(transactionService.getTotalTransferredAmount(1L, 1)).thenReturn(new BigDecimal("1000.00"));

        Map<String, Object> limitResponse = Map.of(
                "dailyTotal", new BigDecimal("1000.00"),
                "dailyLimit", new BigDecimal("5000"),
                "remaining", new BigDecimal("4000"),
                "limitExceeded", false
        );

        Map<String, Object> responseBody = Map.of(
                "message", "Daily limit checked successfully",
                "data", limitResponse
        );
        when(responseUtil.successResponse(eq("Daily limit checked successfully"), any(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/transactions/card/{cardId}/daily-limit", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Daily limit checked successfully"))
                .andExpect(jsonPath("$.data.dailyTotal").value(1000.00))
                .andExpect(jsonPath("$.data.remaining").value(4000.00))
                .andExpect(jsonPath("$.data.limitExceeded").value(false));

        verify(authService).getCurrentUserId();
        verify(cardService).getCardByIdAndOwnerId(1L, 1L);
        verify(transactionService).getTotalTransferredAmount(1L, 1);
    }

    @Test
    @WithMockUser(roles = "USER")
    void checkDailyLimit_WhenLimitExceeded_ShouldReturnExceeded() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);
        when(cardService.getCardByIdAndOwnerId(1L, 1L)).thenReturn(testCard);
        when(transactionService.getTotalTransferredAmount(1L, 1)).thenReturn(new BigDecimal("6000.00"));

        Map<String, Object> limitResponse = Map.of(
                "dailyTotal", new BigDecimal("6000.00"),
                "dailyLimit", new BigDecimal("5000"),
                "remaining", new BigDecimal("-1000"),
                "limitExceeded", true
        );

        Map<String, Object> responseBody = Map.of(
                "message", "Daily limit checked successfully",
                "data", limitResponse
        );
        when(responseUtil.successResponse(eq("Daily limit checked successfully"), any(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/transactions/card/{cardId}/daily-limit", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limitExceeded").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void transferBetweenCards_ShouldWorkForAdminRole() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);
        when(transactionService.transferBetweenOwnCards(any(TransactionDTO.TransferRequest.class), eq(1L)))
                .thenReturn(testTransaction);
        when(transactionMapper.toResponse(testTransaction)).thenReturn(transactionResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "Transfer completed successfully",
                "data", transactionResponse
        );
        when(responseUtil.createdResponse(eq("Transfer completed successfully"), any(TransactionDTO.Response.class)))
                .thenReturn(ResponseEntity.status(201).body(responseBody));

        mockMvc.perform(post("/api/transactions/transfer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated());

        verify(authService).getCurrentUserId();
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserTransactions_WithDefaultPagination_ShouldUseDefaults() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(1L);

        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction),
                PageRequest.of(0, 20, Sort.Direction.DESC, "createdAt"), 1);

        when(transactionService.getUserTransactions(1L, 0, 20)).thenReturn(transactionPage);
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(transactionResponse);

        Map<String, Object> paginatedResponse = Map.of(
                "items", List.of(transactionResponse),
                "currentPage", 0,
                "totalItems", 1L,
                "totalPages", 1
        );

        Map<String, Object> responseBody = Map.of(
                "message", "Transactions retrieved successfully",
                "data", paginatedResponse
        );

        when(responseUtil.successResponse(eq("Transactions retrieved successfully"), any()))
                .thenReturn(ResponseEntity.ok(responseBody));

        when(responseUtil.paginatedResponse(anyList(), eq(0), eq(20), eq(1L), eq(1)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk());

        verify(transactionService).getUserTransactions(1L, 0, 20);
    }
}