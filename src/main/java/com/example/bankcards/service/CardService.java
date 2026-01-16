package com.example.bankcards.service;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.UserOperationException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMaskingUtil;
import com.example.bankcards.util.CardNumberGenerator;
import com.example.bankcards.util.EncryptionUtil;
import com.example.bankcards.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final CardMaskingUtil cardMaskingUtil;
    private final CardNumberGenerator cardNumberGenerator;
    private final ValidationUtil validationUtil;

    @Transactional
    public Card createCard(CardDTO.CreateRequest request) {
        User owner = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserOperationException("User not found with id: " + request.getUserId(), HttpStatus.NOT_FOUND));

        if (!cardNumberGenerator.validateLuhn(request.getCardNumber())){
            throw new CardOperationException("Invalid card number", HttpStatus.BAD_REQUEST);
        }

        if (!cardNumberGenerator.isValidExpiryDate(request.getExpiryDate())){
            throw new CardOperationException("Invalid expire date", HttpStatus.BAD_REQUEST);
        }


        if (!validationUtil.isNotExpired(request.getExpiryDate())) {
            throw new CardOperationException("Card is already expired", HttpStatus.BAD_REQUEST);
        }

        if (!validationUtil.isValidCardNumber(request.getCardNumber())) {
            throw new CardOperationException("Invalid card number", HttpStatus.BAD_REQUEST);
        }

        String encryptedCardNumber = encryptionUtil.encrypt(request.getCardNumber());
        String cardNumberHash = encryptionUtil.hash(request.getCardNumber());

        if (cardRepository.existsByCardNumberHash(cardNumberHash)) {
            throw new CardOperationException("Card with this number already exists");
        }

        String maskedNumber = cardMaskingUtil.validateAndMask(request.getCardNumber());
        String encryptedCVV = encryptionUtil.encrypt(request.getCvv());

        Card card = Card.builder()
                .cardNumber(encryptedCardNumber)
                .cardNumberHash(cardNumberHash)
                .maskedNumber(maskedNumber)
                .ownerName(request.getOwnerName())
                .expiryDate(request.getExpiryDate())
                .cvv(encryptedCVV)
                .balance(request.getInitialBalance() != null ?
                        request.getInitialBalance() : BigDecimal.ZERO)
                .status(CardStatus.ACTIVE)
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .build();

        return cardRepository.save(card);
    }

    @Transactional
    public Card generateNewCard(Long userId, String ownerName) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new UserOperationException("User not found with id: " + userId, HttpStatus.NOT_FOUND));

        String cardNumber = cardNumberGenerator.generateCardNumber(null);
        String cvv = cardNumberGenerator.generateCVV();
        LocalDate expiryDate = cardNumberGenerator.generateExpiryDate();

        CardDTO.CreateRequest request = CardDTO.CreateRequest
                .builder()
                .cardNumber(cardNumber)
                .ownerName(ownerName)
                .expiryDate(expiryDate)
                .cvv(cvv)
                .userId(userId)
                .initialBalance(BigDecimal.ZERO)
                .build();
        return createCard(request);
    }

    @Transactional(readOnly = true)
    public Card getCardById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new UserOperationException("User not found with id: " + id, HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Card getCardByIdAndOwnerId(Long cardId, Long ownerId) {
        Card card = getCardById(cardId);
        if (!card.getOwner().getId().equals(ownerId)) {
            throw new CardOperationException("Card does not belong to user");
        }
        return card;
    }


    @Transactional(readOnly = true)
    public List<Card> getUserCards(Long userId) {
        return cardRepository.findByOwnerId(userId);
    }

    @Transactional(readOnly = true)
    public Page<Card> getUserCards(Long userId, CardDTO.FilterRequest filter) {
        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.Direction.fromString(filter.getSortDirection()),
                filter.getSortBy()
        );

        if (filter.getStatus() != null || filter.getCardNumberLastFour() != null) {
            return cardRepository.findCardsWithFilters(
                    userId,
                    filter.getStatus(),
                    filter.getCardNumberLastFour(),
                    pageable
            );
        }

        return cardRepository.findByOwnerId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Card> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }

    @Transactional
    public Card updateCardStatus(Long cardId, CardDTO.UpdateStatusRequest request) {
        Card card = getCardById(cardId);

        if (card.isExpired() && request.getStatus() == CardStatus.ACTIVE) {
            throw new CardOperationException("Cannot activate expired card");
        }

        card.setStatus(request.getStatus());
        card.setUpdatedAt(LocalDateTime.now());

        log.info("Card {} status updated to {}", cardId, request.getStatus());
        return cardRepository.save(card);
    }

    @Transactional
    public Card blockCard(Long cardId, String reason) {
        CardDTO.UpdateStatusRequest request =
                CardDTO.UpdateStatusRequest.builder()
                        .status(CardStatus.BLOCKED)
                        .reason(reason)
                        .build();
        return updateCardStatus(cardId, request);
    }

    @Transactional
    public Card activateCard(Long cardId) {
        Card card = getCardById(cardId);

        if (card.isExpired()) {
            throw new CardOperationException("Cannot activate expired card");
        }

        CardDTO.UpdateStatusRequest request = CardDTO.UpdateStatusRequest
                .builder()
                .status(CardStatus.ACTIVE)
                .build();
        return updateCardStatus(cardId, request);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        Card card = getCardById(cardId);

        if (card.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new CardOperationException("Cannot delete card with positive balance");
        }

        cardRepository.delete(card);
        log.info("Card deleted: {}", cardId);
    }

    @Transactional
    public void updateExpiredCardsStatus() {
        List<Card> expiredCards = cardRepository.findExpiredCards(LocalDate.now());

        for (Card card : expiredCards) {
            if (card.getStatus() != CardStatus.EXPIRED) {
                card.setStatus(CardStatus.EXPIRED);
                card.setUpdatedAt(LocalDateTime.now());
                cardRepository.save(card);
                log.info("Card {} marked as expired", card.getId());
            }
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getCardBalance(Long cardId) {
        Card card = getCardById(cardId);
        return card.getBalance();
    }

    @Transactional
    public void updateBalance(Long cardId, BigDecimal newBalance) {
        Card card = getCardById(cardId);
        card.setBalance(newBalance);
        card.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(card);
    }
}
