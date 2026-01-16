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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private CardMaskingUtil cardMaskingUtil;

    @Mock
    private CardNumberGenerator cardNumberGenerator;

    @Mock
    private ValidationUtil validationUtil;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Card testCard;
    private CardDTO.CreateRequest createRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        testCard = Card.builder()
                .id(1L)
                .cardNumber("encrypted-1234")
                .cardNumberHash("hash-1234")
                .maskedNumber("1234****5678")
                .ownerName("IVAN IVANOV")
                .expiryDate(LocalDate.now().plusYears(2))
                .cvv("encrypted-123")
                .balance(new BigDecimal("1000.00"))
                .status(CardStatus.ACTIVE)
                .owner(testUser)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = CardDTO.CreateRequest.builder()
                .cardNumber("1234567812345678")
                .ownerName("IVAN IVANOV")
                .expiryDate(LocalDate.now().plusYears(2))
                .cvv("123")
                .userId(1L)
                .initialBalance(new BigDecimal("1000.00"))
                .build();
    }

    @Test
    void createCard_ShouldCreateCardSuccessfully() {

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardNumberGenerator.validateLuhn("1234567812345678")).thenReturn(true);
        when(cardNumberGenerator.isValidExpiryDate(any(LocalDate.class))).thenReturn(true);
        when(validationUtil.isNotExpired(any(LocalDate.class))).thenReturn(true);
        when(validationUtil.isValidCardNumber("1234567812345678")).thenReturn(true);
        when(encryptionUtil.encrypt("1234567812345678")).thenReturn("encrypted-1234");
        when(encryptionUtil.hash("1234567812345678")).thenReturn("hash-1234");
        when(cardRepository.existsByCardNumberHash("hash-1234")).thenReturn(false);
        when(cardMaskingUtil.validateAndMask("1234567812345678")).thenReturn("1234****5678");
        when(encryptionUtil.encrypt("123")).thenReturn("encrypted-123");
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.createCard(createRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userRepository).findById(1L);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserOperationException.class, () ->
                cardService.createCard(createRequest));
    }

    @Test
    void createCard_ShouldThrowException_WhenCardNumberInvalid() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardNumberGenerator.validateLuhn("1234567812345678")).thenReturn(false);

        assertThrows(CardOperationException.class, () ->
                cardService.createCard(createRequest));
    }

    @Test
    void createCard_ShouldThrowException_WhenCardAlreadyExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardNumberGenerator.validateLuhn("1234567812345678")).thenReturn(true);
        when(cardNumberGenerator.isValidExpiryDate(any(LocalDate.class))).thenReturn(true);
        when(validationUtil.isNotExpired(any(LocalDate.class))).thenReturn(true);
        when(validationUtil.isValidCardNumber("1234567812345678")).thenReturn(true);
        when(encryptionUtil.hash("1234567812345678")).thenReturn("hash-1234");
        when(cardRepository.existsByCardNumberHash("hash-1234")).thenReturn(true);

        assertThrows(CardOperationException.class, () ->
                cardService.createCard(createRequest));
    }

    @Test
    void getCardById_ShouldReturnCard() {

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        Card result = cardService.getCardById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(cardRepository).findById(1L);
    }

    @Test
    void getCardById_ShouldThrowException_WhenCardNotFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserOperationException.class, () ->
                cardService.getCardById(1L));
    }

    @Test
    void getCardByIdAndOwnerId_ShouldReturnCard_WhenOwnerMatches() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        Card result = cardService.getCardByIdAndOwnerId(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getOwner().getId());
    }

    @Test
    void getCardByIdAndOwnerId_ShouldThrowException_WhenOwnerDoesNotMatch() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThrows(CardOperationException.class, () ->
                cardService.getCardByIdAndOwnerId(1L, 2L));
    }

    @Test
    void getUserCards_ShouldReturnUserCards() {
        List<Card> cards = List.of(testCard);
        when(cardRepository.findByOwnerId(1L)).thenReturn(cards);

        List<Card> result = cardService.getUserCards(1L);

        assertEquals(1, result.size());
        verify(cardRepository).findByOwnerId(1L);
    }

    @Test
    void getAllCards_ShouldReturnPaginatedCards() {
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));
        Pageable pageable = PageRequest.of(0, 20);

        when(cardRepository.findAll(pageable)).thenReturn(cardPage);

        Page<Card> result = cardService.getAllCards(pageable);

        assertEquals(1, result.getTotalElements());
        verify(cardRepository).findAll(pageable);
    }

    @Test
    void updateCardStatus_ShouldUpdateStatus() {
        CardDTO.UpdateStatusRequest request = CardDTO.UpdateStatusRequest.builder()
                .status(CardStatus.BLOCKED)
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.updateCardStatus(1L, request);

        assertNotNull(result);
        verify(cardRepository).findById(1L);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void updateCardStatus_ShouldThrowException_WhenActivatingExpiredCard() {
        Card expiredCard = Card.builder()
                .id(1L)
                .expiryDate(LocalDate.now().minusDays(1))
                .status(CardStatus.EXPIRED)
                .build();

        CardDTO.UpdateStatusRequest request = CardDTO.UpdateStatusRequest.builder()
                .status(CardStatus.ACTIVE)
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(expiredCard));

        assertThrows(CardOperationException.class, () ->
                cardService.updateCardStatus(1L, request));
    }

    @Test
    void blockCard_ShouldBlockCard() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.blockCard(1L, "Suspicious activity");

        assertNotNull(result);
        verify(cardRepository).findById(1L);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void activateCard_ShouldActivateCard() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.activateCard(1L);

        assertNotNull(result);
        verify(cardRepository, times(2)).findById(1L);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void deleteCard_ShouldDeleteCard_WhenBalanceIsZero() {
        Card cardWithZeroBalance = Card.builder()
                .id(1L)
                .balance(BigDecimal.ZERO)
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(cardWithZeroBalance));

        cardService.deleteCard(1L);

        verify(cardRepository).findById(1L);
        verify(cardRepository).delete(cardWithZeroBalance);
    }

    @Test
    void deleteCard_ShouldThrowException_WhenBalanceIsPositive() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThrows(CardOperationException.class, () ->
                cardService.deleteCard(1L));
    }

    @Test
    void getCardBalance_ShouldReturnBalance() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        BigDecimal balance = cardService.getCardBalance(1L);

        assertEquals(new BigDecimal("1000.00"), balance);
        verify(cardRepository).findById(1L);
    }

    @Test
    void generateNewCard_ShouldGenerateCard() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardNumberGenerator.generateCardNumber(null)).thenReturn("1234567812345678");
        when(cardNumberGenerator.generateCVV()).thenReturn("123");
        when(cardNumberGenerator.generateExpiryDate()).thenReturn(LocalDate.now().plusYears(2));

        when(cardNumberGenerator.validateLuhn(anyString())).thenReturn(true);
        when(cardNumberGenerator.isValidExpiryDate(any(LocalDate.class))).thenReturn(true);
        when(validationUtil.isNotExpired(any(LocalDate.class))).thenReturn(true);
        when(validationUtil.isValidCardNumber(anyString())).thenReturn(true);
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted");
        when(encryptionUtil.hash(anyString())).thenReturn("hash");
        when(cardRepository.existsByCardNumberHash(anyString())).thenReturn(false);
        when(cardMaskingUtil.validateAndMask(anyString())).thenReturn("1234****5678");
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.generateNewCard(1L, "NEW USER");

        assertNotNull(result);
        verify(userRepository,times(2)).findById(1L);
        verify(cardNumberGenerator).generateCardNumber(null);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void updateExpiredCardsStatus_ShouldUpdateExpiredCards() {
        List<Card> expiredCards = List.of(testCard);
        when(cardRepository.findExpiredCards(any(LocalDate.class))).thenReturn(expiredCards);

        cardService.updateExpiredCardsStatus();

        verify(cardRepository).findExpiredCards(any(LocalDate.class));
        verify(cardRepository).save(any(Card.class));
    }
}