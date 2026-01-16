package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.ResponseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CardService cardService;

    @Mock
    private AuthService authService;

    @Mock
    private ResponseUtil responseUtil;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardController cardController;

    private ObjectMapper objectMapper;
    private Card testCard;
    private CardDTO.Response testCardResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cardController).build();
        objectMapper = new ObjectMapper();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        testCard = Card.builder()
                .id(1L)
                .cardNumber("encrypted-1234567812345678")
                .cardNumberHash("hash-1234567812345678")
                .maskedNumber("414947******7890")
                .ownerName("IVAN IVANOV")
                .expiryDate(LocalDate.now().plusYears(2))
                .cvv("encrypted-123")
                .balance(new BigDecimal("1000.00"))
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .owner(testUser)
                .build();

        testCardResponse = CardDTO.Response.builder()
                .id(1L)
                .maskedNumber("414947******7890")
                .ownerName("IVAN IVANOV")
                .expiryDate("12/27")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .expired(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .userId(1L)
                .username("testuser")
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserCards_ShouldReturnUserCards() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);

        CardDTO.FilterRequest filterRequest = new CardDTO.FilterRequest();
        filterRequest.setPage(0);
        filterRequest.setSize(10);

        Page<Card> cardPage = new PageImpl<>(List.of(testCard),
                PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt"), 1);

        when(cardService.getUserCards(eq(1L), any(CardDTO.FilterRequest.class))).thenReturn(cardPage);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(testCardResponse);

        Map<String, Object> paginatedResponse = Map.of(
                "items", List.of(testCardResponse),
                "currentPage", 0,
                "totalItems", 1L,
                "totalPages", 1
        );

        Map<String, Object> responseBody = Map.of(
                "message", "Cards retrieved successfully",
                "data", paginatedResponse
        );

        when(responseUtil.successResponse(eq("Cards retrieved successfully"), any()))
                .thenReturn(ResponseEntity.ok(responseBody));

        when(responseUtil.paginatedResponse(anyList(), eq(0), eq(10), eq(1L), eq(1)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/cards")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cards retrieved successfully"))
                .andExpect(jsonPath("$.data.items[0].id").value(1));

        verify(authService).getCurrentUserId();
        verify(cardService).getUserCards(eq(1L), any(CardDTO.FilterRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserCards_ShouldUseDefaultPagination() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);

        Page<Card> cardPage = new PageImpl<>(List.of(testCard),
                PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt"), 1);

        when(cardService.getUserCards(eq(1L), any(CardDTO.FilterRequest.class))).thenReturn(cardPage);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(testCardResponse);

        Map<String, Object> paginatedResponse = Map.of(
                "items", List.of(testCardResponse),
                "currentPage", 0,
                "totalItems", 1L,
                "totalPages", 1
        );

        Map<String, Object> responseBody = Map.of(
                "message", "Cards retrieved successfully",
                "data", paginatedResponse
        );

        when(responseUtil.successResponse(eq("Cards retrieved successfully"), any()))
                .thenReturn(ResponseEntity.ok(responseBody));

        when(responseUtil.paginatedResponse(anyList(), eq(0), eq(10), eq(1L), eq(1)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk());

        verify(cardService).getUserCards(eq(1L), argThat(filter ->
                filter.getPage() == 0 && filter.getSize() == 10));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCard_ShouldReturnCard() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);
        when(cardService.getCardByIdAndOwnerId(1L, 1L)).thenReturn(testCard);
        when(cardMapper.toResponse(testCard)).thenReturn(testCardResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "Card retrieved successfully",
                "data", testCardResponse
        );
        when(responseUtil.successResponse(eq("Card retrieved successfully"), any(CardDTO.Response.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/cards/{cardId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.maskedNumber").value("414947******7890"));

        verify(authService).getCurrentUserId();
        verify(cardService).getCardByIdAndOwnerId(1L, 1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCardBalance_ShouldReturnBalance() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);
        when(cardService.getCardByIdAndOwnerId(1L, 1L)).thenReturn(testCard);
        when(cardService.getCardBalance(1L)).thenReturn(new BigDecimal("1000.00"));

        Map<String, Object> responseBody = Map.of(
                "message", "Balance retrieved successfully",
                "data", new BigDecimal("1000.00")
        );
        when(responseUtil.successResponse(eq("Balance retrieved successfully"), any(BigDecimal.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/cards/{cardId}/balance", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Balance retrieved successfully"))
                .andExpect(jsonPath("$.data").value(1000.00));

        verify(authService).getCurrentUserId();
        verify(cardService).getCardByIdAndOwnerId(1L, 1L);
        verify(cardService).getCardBalance(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void requestBlockCard_ShouldBlockCard() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);

        Card blockedCard = Card.builder()
                .id(1L)
                .status(CardStatus.BLOCKED)
                .build();

        CardDTO.Response blockedResponse = CardDTO.Response.builder()
                .id(1L)
                .status(CardStatus.BLOCKED)
                .build();

        when(cardService.getCardByIdAndOwnerId(1L, 1L)).thenReturn(testCard);
        when(cardService.blockCard(eq(1L), anyString())).thenReturn(blockedCard);
        when(cardMapper.toResponse(blockedCard)).thenReturn(blockedResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "Card block requested successfully",
                "data", blockedResponse
        );
        when(responseUtil.successResponse(eq("Card block requested successfully"), any(CardDTO.Response.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(post("/api/cards/request-block/{cardId}", 1L)
                        .with(csrf())
                        .param("reason", "Lost card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card block requested successfully"));

        verify(authService).getCurrentUserId();
        verify(cardService).getCardByIdAndOwnerId(1L, 1L);
        verify(cardService).blockCard(1L, "Lost card");
    }

    @Test
    @WithMockUser(roles = "USER")
    void requestBlockCard_ShouldUseDefaultReason_WhenNoReasonProvided() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);

        Card blockedCard = Card.builder()
                .id(1L)
                .status(CardStatus.BLOCKED)
                .build();

        when(cardService.getCardByIdAndOwnerId(1L, 1L)).thenReturn(testCard);
        when(cardService.blockCard(eq(1L), eq("Requested by user"))).thenReturn(blockedCard);

        mockMvc.perform(post("/api/cards/request-block/{cardId}", 1L)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(cardService).blockCard(1L, "Requested by user");
    }

    @Test
    @WithMockUser(roles = "USER")
    void activateCard_ShouldActivateCard() throws Exception {
        // Arrange
        when(authService.getCurrentUserId()).thenReturn(1L);

        Card activatedCard = Card.builder()
                .id(1L)
                .status(CardStatus.ACTIVE)
                .build();

        CardDTO.Response activatedResponse = CardDTO.Response.builder()
                .id(1L)
                .status(CardStatus.ACTIVE)
                .build();

        when(cardService.getCardByIdAndOwnerId(1L, 1L)).thenReturn(testCard);
        when(cardService.activateCard(1L)).thenReturn(activatedCard);
        when(cardMapper.toResponse(activatedCard)).thenReturn(activatedResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "Card activated successfully",
                "data", activatedResponse
        );
        when(responseUtil.successResponse(eq("Card activated successfully"), any(CardDTO.Response.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(post("/api/cards/{cardId}/activate", 1L)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card activated successfully"));

        verify(authService).getCurrentUserId();
        verify(cardService).getCardByIdAndOwnerId(1L, 1L);
        verify(cardService).activateCard(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void generateCard_ShouldCreateNewCard() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);

        Card newCard = Card.builder()
                .id(2L)
                .ownerName("NEW USER")
                .status(CardStatus.ACTIVE)
                .build();

        CardDTO.Response newCardResponse = CardDTO.Response.builder()
                .id(2L)
                .ownerName("NEW USER")
                .status(CardStatus.ACTIVE)
                .build();

        when(cardService.generateNewCard(1L, "NEW USER")).thenReturn(newCard);
        when(cardMapper.toResponse(newCard)).thenReturn(newCardResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "Card generated successfully",
                "data", newCardResponse
        );
        when(responseUtil.createdResponse(eq("Card generated successfully"), any(CardDTO.Response.class)))
                .thenReturn(ResponseEntity.status(201).body(responseBody));

        mockMvc.perform(post("/api/cards/generate")
                        .with(csrf())
                        .param("ownerName", "NEW USER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Card generated successfully"));

        verify(authService).getCurrentUserId();
        verify(cardService).generateNewCard(1L, "NEW USER");
    }

    @Test
    @WithMockUser(roles = "USER")
    void generateCard_ShouldReturnBadRequest_WhenOwnerNameMissing() throws Exception {
        mockMvc.perform(post("/api/cards/generate")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void requestBlockCard_ShouldReturnBadRequest_WhenCardNotActive() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(1L);

        Card blockedCard = Card.builder()
                .id(1L)
                .status(CardStatus.BLOCKED)
                .build();

        when(cardService.getCardByIdAndOwnerId(1L, 1L)).thenReturn(blockedCard);

        Map<String, Object> responseBody = Map.of(
                "message", "Card is not active",
                "error", "Cannot block inactive card"
        );
        when(responseUtil.errorResponse(any(), eq("Card is not active"), eq("Cannot block inactive card")))
                .thenReturn(ResponseEntity.badRequest().body(responseBody));

        mockMvc.perform(post("/api/cards/request-block/{cardId}", 1L)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Card is not active"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserCards_ShouldWorkForAdminRole() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);

        Page<Card> cardPage = new PageImpl<>(List.of(testCard),
                PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt"), 1);

        when(cardService.getUserCards(eq(1L), any(CardDTO.FilterRequest.class))).thenReturn(cardPage);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(testCardResponse);

        Map<String, Object> paginatedResponse = Map.of(
                "items", List.of(testCardResponse),
                "currentPage", 0,
                "totalItems", 1L,
                "totalPages", 1
        );

        Map<String, Object> responseBody = Map.of(
                "message", "Cards retrieved successfully",
                "data", paginatedResponse
        );

        when(responseUtil.successResponse(eq("Cards retrieved successfully"), any()))
                .thenReturn(ResponseEntity.ok(responseBody));

        when(responseUtil.paginatedResponse(anyList(), eq(0), eq(10), eq(1L), eq(1)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk());

        verify(authService).getCurrentUserId();
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserCards_WithFilter_ShouldReturnFilteredCards() throws Exception {

        when(authService.getCurrentUserId()).thenReturn(1L);

        Page<Card> cardPage = new PageImpl<>(List.of(testCard),
                PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt"), 1);

        when(cardService.getUserCards(eq(1L), any(CardDTO.FilterRequest.class))).thenReturn(cardPage);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(testCardResponse);

        Map<String, Object> paginatedResponse = Map.of(
                "items", List.of(testCardResponse),
                "currentPage", 0,
                "totalItems", 1L,
                "totalPages", 1
        );

        Map<String, Object> responseBody = Map.of(
                "message", "Cards retrieved successfully",
                "data", paginatedResponse
        );

        when(responseUtil.successResponse(eq("Cards retrieved successfully"), any()))
                .thenReturn(ResponseEntity.ok(responseBody));

        when(responseUtil.paginatedResponse(anyList(), eq(0), eq(10), eq(1L), eq(1)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/cards")
                        .param("status", "ACTIVE")
                        .param("cardNumberLastFour", "7890")
                        .param("ownerName", "IVAN"))
                .andExpect(status().isOk());

        verify(cardService).getUserCards(eq(1L), argThat(filter ->
                filter.getStatus() == CardStatus.ACTIVE &&
                        filter.getCardNumberLastFour().equals("7890") &&
                        filter.getOwnerName().equals("IVAN")
        ));
    }
}