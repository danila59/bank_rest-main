package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@Tag(name = "Admin Cards", description = "API для административного управления картами")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminCardController {

    private final CardService cardService;
    private final ResponseUtil responseUtil;
    private final CardMapper cardMapper;

    @PostMapping
    @Operation(summary = "Создать карту", description = "Создать новую карту для пользователя (только для администратора)")
    public ResponseEntity<?> createCard(@Valid @RequestBody CardDTO.CreateRequest request) {
        Card card = cardService.createCard(request);
        CardDTO.Response response = cardMapper.toResponse(card);
        return responseUtil.createdResponse("Card created successfully", response);
    }

    @GetMapping
    @Operation(summary = "Получить все карты", description = "Получить список всех карт в системе (с пагинацией)")
    public ResponseEntity<?> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<Card> cards = cardService.getAllCards(pageable);

        Page<CardDTO.Response> responses = cards.map(cardMapper::toResponse);
        return responseUtil.successResponse(
                "All cards retrieved successfully",
                responseUtil.paginatedResponse(
                        responses.getContent(),
                        page,
                        size,
                        responses.getTotalElements(),
                        responses.getTotalPages()
                )
        );
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Получить карту по ID", description = "Получить информацию о любой карте в системе")
    public ResponseEntity<?> getCard(@PathVariable Long cardId) {
        Card card = cardService.getCardById(cardId);
        CardDTO.Response response = cardMapper.toResponse(card);
        return responseUtil.successResponse("Card retrieved successfully", response);
    }

    @PutMapping("/{cardId}/status")
    @Operation(summary = "Изменить статус карты", description = "Изменить статус карты (активировать/заблокировать)")
    public ResponseEntity<?> updateCardStatus(
            @PathVariable Long cardId,
            @Valid @RequestBody CardDTO.UpdateStatusRequest request) {

        Card updatedCard = cardService.updateCardStatus(cardId, request);
        CardDTO.Response response = cardMapper.toResponse(updatedCard);
        return responseUtil.successResponse("Card status updated successfully", response);
    }

    @DeleteMapping("/{cardId}")
    @Operation(summary = "Удалить карту", description = "Удалить карту из системы (только если баланс = 0)")
    public ResponseEntity<?> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return responseUtil.successResponse("Card deleted successfully", null);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Получить карты пользователя", description = "Получить все карты конкретного пользователя")
    public ResponseEntity<?> getUserCards(@PathVariable Long userId) {
        List<Card> cards = cardService.getUserCards(userId);
        List<CardDTO.Response> response = cards.stream().map(cardMapper::toResponse).toList();
        return responseUtil.successResponse("User cards retrieved successfully", response);
    }

    @PostMapping("/{cardId}/force-block")
    @Operation(summary = "Принудительно заблокировать карту", description = "Блокировка карты администратором")
    public ResponseEntity<?> forceBlockCard(
            @PathVariable Long cardId,
            @RequestParam String reason) {

        Card blockedCard = cardService.blockCard(cardId, reason);
        CardDTO.Response response = cardMapper.toResponse(blockedCard);
        return responseUtil.successResponse("Card force blocked successfully", response);
    }

    @PostMapping("/check-expired")
    @Operation(summary = "Проверить истекшие карты", description = "Запустить проверку истекших карт")
    public ResponseEntity<?> checkExpiredCards() {
        cardService.updateExpiredCardsStatus();
        return responseUtil.successResponse("Expired cards check completed", null);
    }
}