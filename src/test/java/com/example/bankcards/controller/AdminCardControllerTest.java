package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.ResponseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
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
class AdminCardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CardService cardService;

    @Mock
    private ResponseUtil responseUtil;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private AdminCardController adminCardController;

    private ObjectMapper objectMapper;
    private Card testCard;
    private CardDTO.Response testCardResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminCardController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        testCard = Card.builder()
                .id(1L)
                .cardNumber("encrypted-1234567812345678")
                .maskedNumber("1234****5678")
                .ownerName("IVAN IVANOV")
                .expiryDate(LocalDate.now().plusYears(2))
                .cvv("encrypted-123")
                .balance(new BigDecimal("1000.00"))
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .owner(testUser)
                .build();

        testCardResponse = CardDTO.Response.builder()
                .id(1L)
                .maskedNumber("1234****5678")
                .ownerName("IVAN IVANOV")
                .expiryDate("12/27")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .expired(false)
                .createdAt(LocalDateTime.now())
                .userId(1L)
                .username("testuser")
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCards_ShouldReturnCards() throws Exception {

        Page<Card> cardPage = new PageImpl<>(List.of(testCard),
                PageRequest.of(0, 20, Sort.Direction.DESC, "createdAt"), 1);

        when(cardService.getAllCards(any(Pageable.class))).thenReturn(cardPage);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(testCardResponse);

        Map<String, Object> paginatedResponse = Map.of(
                "items", List.of(testCardResponse),
                "currentPage", 0,
                "totalItems", 1L,
                "totalPages", 1
        );

        Map<String, Object> responseBody = Map.of(
                "message", "All cards retrieved successfully",
                "data", paginatedResponse
        );

        when(responseUtil.successResponse(eq("All cards retrieved successfully"), any()))
                .thenReturn(ResponseEntity.ok(responseBody));

        when(responseUtil.paginatedResponse(anyList(), anyInt(), anyInt(), anyLong(), anyInt()))
                .thenReturn(paginatedResponse);


        mockMvc.perform(get("/api/admin/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All cards retrieved successfully"))
                .andExpect(jsonPath("$.data.items[0].id").value(1));

        verify(cardService).getAllCards(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCards_WithCustomPagination_ShouldUseParameters() throws Exception {
        // Arrange
        Page<Card> cardPage = new PageImpl<>(List.of(testCard),
                PageRequest.of(2, 50, Sort.Direction.ASC, "balance"), 1);

        when(cardService.getAllCards(any(Pageable.class))).thenReturn(cardPage);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(testCardResponse);

        Map<String, Object> paginatedResponse = Map.of(
                "items", List.of(testCardResponse),
                "currentPage", 2,
                "totalItems", 1L,
                "totalPages", 1
        );

        Map<String, Object> responseBody = Map.of(
                "message", "All cards retrieved successfully",
                "data", paginatedResponse
        );

        when(responseUtil.successResponse(eq("All cards retrieved successfully"), any()))
                .thenReturn(ResponseEntity.ok(responseBody));

        when(responseUtil.paginatedResponse(anyList(), anyInt(), anyInt(), anyLong(), anyInt()))
                .thenReturn(paginatedResponse);


        mockMvc.perform(get("/api/admin/cards")
                        .param("page", "2")
                        .param("size", "50")
                        .param("sortBy", "balance")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk());

        verify(cardService).getAllCards(argThat(pageable ->
                pageable.getPageNumber() == 2 &&
                        pageable.getPageSize() == 50 &&
                        pageable.getSort().getOrderFor("balance").getDirection() == Sort.Direction.ASC
        ));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCard_ShouldReturnCard() throws Exception {

        when(cardService.getCardById(1L)).thenReturn(testCard);
        when(cardMapper.toResponse(testCard)).thenReturn(testCardResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "Card retrieved successfully",
                "data", testCardResponse
        );
        when(responseUtil.successResponse(eq("Card retrieved successfully"), any(CardDTO.Response.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/admin/cards/{cardId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(cardService).getCardById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCardStatus_ShouldUpdateStatus() throws Exception {
        // Arrange
        CardDTO.UpdateStatusRequest request = CardDTO.UpdateStatusRequest.builder()
                .status(CardStatus.BLOCKED)
                .reason("Suspicious activity")
                .build();

        Card blockedCard = Card.builder()
                .id(1L)
                .status(CardStatus.BLOCKED)
                .build();

        CardDTO.Response blockedResponse = CardDTO.Response.builder()
                .id(1L)
                .status(CardStatus.BLOCKED)
                .build();

        when(cardService.updateCardStatus(eq(1L), any(CardDTO.UpdateStatusRequest.class))).thenReturn(blockedCard);
        when(cardMapper.toResponse(blockedCard)).thenReturn(blockedResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "Card status updated successfully",
                "data", blockedResponse
        );
        when(responseUtil.successResponse(eq("Card status updated successfully"), any(CardDTO.Response.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(put("/api/admin/cards/{cardId}/status", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card status updated successfully"));

        verify(cardService).updateCardStatus(eq(1L), any(CardDTO.UpdateStatusRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCard_ShouldDeleteCard() throws Exception {

        doNothing().when(cardService).deleteCard(1L);

        Map<String, Object> responseBody = Map.of(
                "message", "Card deleted successfully"
        );
        when(responseUtil.successResponse(eq("Card deleted successfully"), isNull()))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(delete("/api/admin/cards/{cardId}", 1L)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card deleted successfully"));

        verify(cardService).deleteCard(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserCards_ShouldReturnUserCards() throws Exception {

        List<Card> cards = List.of(testCard);
        List<CardDTO.Response> responses = List.of(testCardResponse);

        when(cardService.getUserCards(1L)).thenReturn(cards);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(testCardResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "User cards retrieved successfully",
                "data", responses
        );
        when(responseUtil.successResponse(eq("User cards retrieved successfully"), any(List.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/admin/cards/user/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User cards retrieved successfully"))
                .andExpect(jsonPath("$.data[0].userId").value(1));

        verify(cardService).getUserCards(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void forceBlockCard_ShouldBlockCardWithReason() throws Exception {

        Card blockedCard = Card.builder()
                .id(1L)
                .status(CardStatus.BLOCKED)
                .build();

        CardDTO.Response blockedResponse = CardDTO.Response.builder()
                .id(1L)
                .status(CardStatus.BLOCKED)
                .build();

        when(cardService.blockCard(1L, "Security reasons")).thenReturn(blockedCard);
        when(cardMapper.toResponse(blockedCard)).thenReturn(blockedResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "Card force blocked successfully",
                "data", blockedResponse
        );
        when(responseUtil.successResponse(eq("Card force blocked successfully"), any(CardDTO.Response.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(post("/api/admin/cards/{cardId}/force-block", 1L)
                        .with(csrf())
                        .param("reason", "Security reasons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card force blocked successfully"));

        verify(cardService).blockCard(1L, "Security reasons");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkExpiredCards_ShouldProcessExpiredCards() throws Exception {

        doNothing().when(cardService).updateExpiredCardsStatus();

        Map<String, Object> responseBody = Map.of(
                "message", "Expired cards check completed"
        );
        when(responseUtil.successResponse(eq("Expired cards check completed"), isNull()))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(post("/api/admin/cards/check-expired")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Expired cards check completed"));

        verify(cardService).updateExpiredCardsStatus();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCardStatus_WithInvalidStatus_ShouldReturnBadRequest() throws Exception {

        String invalidJson = "{\"status\": \"INVALID\"}";

        mockMvc.perform(put("/api/admin/cards/{cardId}/status", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void forceBlockCard_WithoutReason_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/cards/{cardId}/force-block", 1L)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCards_WithDefaultParameters_ShouldUseDefaults() throws Exception {
        Page<Card> cardPage = new PageImpl<>(List.of(testCard),
                PageRequest.of(0, 20, Sort.Direction.DESC, "createdAt"), 1);

        when(cardService.getAllCards(any(Pageable.class))).thenReturn(cardPage);
        when(cardMapper.toResponse(any(Card.class))).thenReturn(testCardResponse);

        Map<String, Object> paginatedResponse = Map.of(
                "items", List.of(testCardResponse),
                "currentPage", 0,
                "totalItems", 1L,
                "totalPages", 1
        );

        Map<String, Object> responseBody = Map.of(
                "message", "All cards retrieved successfully",
                "data", paginatedResponse
        );

        when(responseUtil.successResponse(eq("All cards retrieved successfully"), any()))
                .thenReturn(ResponseEntity.ok(responseBody));

        when(responseUtil.paginatedResponse(anyList(), anyInt(), anyInt(), anyLong(), anyInt()))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/admin/cards"))
                .andExpect(status().isOk());

        verify(cardService).getAllCards(argThat(pageable ->
                pageable.getPageNumber() == 0 &&
                        pageable.getPageSize() == 20 &&
                        pageable.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC
        ));
    }
}