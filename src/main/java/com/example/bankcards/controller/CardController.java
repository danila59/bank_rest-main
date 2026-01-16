package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.CardService;
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

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "API для управления картами")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;
    private final AuthService authService;
    private final ResponseUtil responseUtil;
    private final CardMapper cardMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Получить карты пользователя", description = "Получить список карт текущего пользователя с пагинацией и фильтрацией")
    public ResponseEntity<?> getUserCards(@Valid CardDTO.FilterRequest filterRequest) {
        Long userId = authService.getCurrentUserId();
        Page<Card> cards = cardService.getUserCards(userId, filterRequest);

        Page<CardDTO.Response> responsePage = cards.map(cardMapper::toResponse);

        return responseUtil.successResponse(
                "Cards retrieved successfully",
                responseUtil.paginatedResponse(
                        responsePage.getContent(),
                        filterRequest.getPage(),
                        filterRequest.getSize(),
                        responsePage.getTotalElements(),
                        responsePage.getTotalPages()
                )
        );
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Получить карту по ID", description = "Получить информацию о конкретной карте")
    public ResponseEntity<?> getCard(@PathVariable Long cardId) {
        Long userId = authService.getCurrentUserId();
        Card card = cardService.getCardByIdAndOwnerId(cardId, userId);
        CardDTO.Response response = cardMapper.toResponse(card);
        return responseUtil.successResponse("Card retrieved successfully", response);
    }

    @GetMapping("/{cardId}/balance")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Получить баланс карты", description = "Получить текущий баланс карты")
    public ResponseEntity<?> getCardBalance(@PathVariable Long cardId) {
        Long userId = authService.getCurrentUserId();
        cardService.getCardByIdAndOwnerId(cardId, userId);

        return responseUtil.successResponse(
                "Balance retrieved successfully",
                cardService.getCardBalance(cardId)
        );
    }

    @PostMapping("/request-block/{cardId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Запросить блокировку карты", description = "Пользователь запрашивает блокировку своей карты")
    public ResponseEntity<?> requestBlockCard(
            @PathVariable Long cardId,
            @RequestParam(required = false) String reason) {
        Long userId = authService.getCurrentUserId();
        Card card = cardService.getCardByIdAndOwnerId(cardId, userId);

        if (card.getStatus() != CardStatus.ACTIVE) {
            return responseUtil.errorResponse(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Card is not active",
                    "Cannot block inactive card"
            );
        }

        Card blockedCard = cardService.blockCard(cardId, reason != null ? reason : "Requested by user");
        CardDTO.Response response = cardMapper.toResponse(blockedCard);
        return responseUtil.successResponse("Card block requested successfully", response);
    }

    @PostMapping("/{cardId}/activate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Активировать карту", description = "Активировать заблокированную карту (если не истек срок)")
    public ResponseEntity<?> activateCard(@PathVariable Long cardId) {
        Long userId = authService.getCurrentUserId();
        cardService.getCardByIdAndOwnerId(cardId, userId);

        Card activatedCard = cardService.activateCard(cardId);
        CardDTO.Response response = cardMapper.toResponse(activatedCard);

        return responseUtil.successResponse("Card activated successfully", response);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Сгенерировать новую карту", description = "Создать новую карту для текущего пользователя")
    public ResponseEntity<?> generateCard(@RequestParam String ownerName) {
        Long userId = authService.getCurrentUserId();
        Card newCard = cardService.generateNewCard(userId, ownerName);

        CardDTO.Response response = cardMapper.toResponse(newCard);
        return responseUtil.createdResponse("Card generated successfully", response);
    }
}
