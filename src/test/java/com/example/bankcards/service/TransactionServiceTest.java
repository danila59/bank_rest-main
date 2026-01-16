package com.example.bankcards.service;

import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.TransactionStatus;
import com.example.bankcards.entity.enums.TransactionType;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.TransactionException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.util.EncryptionUtil;
import com.example.bankcards.util.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardService cardService;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private ValidationUtil validationUtil;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Card fromCard;
    private Card toCard;
    private Transaction testTransaction;
    private TransactionDTO.TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        fromCard = Card.builder()
                .id(1L)
                .cardNumber("encrypted-1111")
                .cardNumberHash("hash-1111")
                .maskedNumber("1111****1111")
                .ownerName("FROM USER")
                .balance(new BigDecimal("5000.00"))
                .status(CardStatus.ACTIVE)
                .cvv("encrypted-cvv1")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(1))
                .build();

        toCard = Card.builder()
                .id(2L)
                .cardNumber("encrypted-2222")
                .cardNumberHash("hash-2222")
                .maskedNumber("2222****2222")
                .ownerName("TO USER")
                .balance(new BigDecimal("1000.00"))
                .status(CardStatus.ACTIVE)
                .cvv("encrypted-cvv2")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(1))
                .build();

        testTransaction = Transaction.builder()
                .transactionId("TXN123")
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .description("Test transfer")
                .fromCard(fromCard)
                .toCard(toCard)
                .transactionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        transferRequest = new TransactionDTO.TransferRequest();
        transferRequest.setFromCardNumber("1111111111111111");
        transferRequest.setToCardNumber("2222222222222222");
        transferRequest.setAmount(new BigDecimal("100.00"));
        transferRequest.setDescription("Test transfer");
        transferRequest.setCvv("123");
    }

    @Test
    void transferBetweenOwnCards_ShouldCompleteTransfer() {
        when(validationUtil.isValidAmount(any(BigDecimal.class))).thenReturn(true);
        when(encryptionUtil.hash("1111111111111111")).thenReturn("hash-1111");
        when(encryptionUtil.hash("2222222222222222")).thenReturn("hash-2222");
        when(cardRepository.findByCardNumberHash("hash-1111")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash-2222")).thenReturn(Optional.of(toCard));
        when(encryptionUtil.decrypt("encrypted-cvv1")).thenReturn("123");
        when(transactionRepository.findTotalWithdrawnAmount(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0.0);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        Transaction result = transactionService.transferBetweenOwnCards(transferRequest, 1L);

        assertNotNull(result);
        assertEquals("TXN123", result.getTransactionId());
        assertEquals(new BigDecimal("100.00"), result.getAmount());

        verify(cardService).updateBalance(1L, new BigDecimal("4900.00"));
        verify(cardService).updateBalance(2L, new BigDecimal("1100.00"));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowException_WhenInvalidAmount() {
        when(validationUtil.isValidAmount(any(BigDecimal.class))).thenReturn(false);

        assertThrows(TransactionException.class, () ->
                transactionService.transferBetweenOwnCards(transferRequest, 1L));
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowException_WhenFromCardNotFound() {
        when(validationUtil.isValidAmount(any(BigDecimal.class))).thenReturn(true);
        when(encryptionUtil.hash("1111111111111111")).thenReturn("hash-1111");
        when(cardRepository.findByCardNumberHash("hash-1111")).thenReturn(Optional.empty());

        assertThrows(CardOperationException.class, () ->
                transactionService.transferBetweenOwnCards(transferRequest, 1L));
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowException_WhenCardNotBelongsToUser() {
        User otherUser = User.builder().id(2L).build();
        Card otherUserCard = Card.builder().id(1L).owner(otherUser).build();

        when(validationUtil.isValidAmount(any(BigDecimal.class))).thenReturn(true);
        when(encryptionUtil.hash("1111111111111111")).thenReturn("hash-1111");
        when(cardRepository.findByCardNumberHash("hash-1111")).thenReturn(Optional.of(otherUserCard));

        assertThrows(CardOperationException.class, () ->
                transactionService.transferBetweenOwnCards(transferRequest, 1L));
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowException_WhenInvalidCVV() {
        when(validationUtil.isValidAmount(any(BigDecimal.class))).thenReturn(true);
        when(encryptionUtil.hash("1111111111111111")).thenReturn("hash-1111");
        when(encryptionUtil.hash("2222222222222222")).thenReturn("hash-2222");
        when(cardRepository.findByCardNumberHash("hash-1111")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash-2222")).thenReturn(Optional.of(toCard));
        when(encryptionUtil.decrypt("encrypted-cvv1")).thenReturn("999");

        assertThrows(TransactionException.class, () ->
                transactionService.transferBetweenOwnCards(transferRequest, 1L));
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowException_WhenCardBlocked() {
        Card blockedCard = Card.builder()
                .id(1L)
                .status(CardStatus.BLOCKED)
                .owner(testUser)
                .cvv("encrypted-cvv1")
                .expiryDate(LocalDate.now().plusYears(1))
                .build();

        when(validationUtil.isValidAmount(any(BigDecimal.class))).thenReturn(true);
        when(encryptionUtil.hash("1111111111111111")).thenReturn("hash-1111");
        when(encryptionUtil.hash("2222222222222222")).thenReturn("hash-2222");
        when(cardRepository.findByCardNumberHash("hash-1111")).thenReturn(Optional.of(blockedCard));
        when(cardRepository.findByCardNumberHash("hash-2222")).thenReturn(Optional.of(toCard));

        when(encryptionUtil.decrypt("encrypted-cvv1")).thenReturn("123");
        assertThrows(CardOperationException.class, () ->
                transactionService.transferBetweenOwnCards(transferRequest, 1L));
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowException_WhenInsufficientFunds() {
        Card lowBalanceCard = Card.builder()
                .id(1L)
                .balance(new BigDecimal("50.00"))
                .status(CardStatus.ACTIVE)
                .cvv("encrypted-cvv1")
                .owner(testUser)
                .expiryDate(LocalDate.now().plusYears(1))
                .build();

        when(validationUtil.isValidAmount(any(BigDecimal.class))).thenReturn(true);
        when(encryptionUtil.hash("1111111111111111")).thenReturn("hash-1111");
        when(encryptionUtil.hash("2222222222222222")).thenReturn("hash-2222");
        when(cardRepository.findByCardNumberHash("hash-1111")).thenReturn(Optional.of(lowBalanceCard));
        when(cardRepository.findByCardNumberHash("hash-2222")).thenReturn(Optional.of(toCard));
        when(encryptionUtil.decrypt("encrypted-cvv1")).thenReturn("123");

        when(encryptionUtil.decrypt("encrypted-cvv1")).thenReturn("123");
        assertThrows(TransactionException.class, () ->
                transactionService.transferBetweenOwnCards(transferRequest, 1L));
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowException_WhenDailyLimitExceeded() {
        when(validationUtil.isValidAmount(any(BigDecimal.class))).thenReturn(true);
        when(encryptionUtil.hash("1111111111111111")).thenReturn("hash-1111");
        when(encryptionUtil.hash("2222222222222222")).thenReturn("hash-2222");
        when(cardRepository.findByCardNumberHash("hash-1111")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash-2222")).thenReturn(Optional.of(toCard));
        when(encryptionUtil.decrypt("encrypted-cvv1")).thenReturn("123");
        when(transactionRepository.findTotalWithdrawnAmount(eq(1L), any(LocalDateTime.class)))
                .thenReturn(4950.0);

        assertThrows(TransactionException.class, () ->
                transactionService.transferBetweenOwnCards(transferRequest, 1L));
    }

    @Test
    void getTransactionById_ShouldReturnTransaction() {
        when(transactionRepository.findByTransactionId("TXN123")).thenReturn(Optional.of(testTransaction));

        Transaction result = transactionService.getTransactionById("TXN123");

        assertNotNull(result);
        assertEquals("TXN123", result.getTransactionId());
        verify(transactionRepository).findByTransactionId("TXN123");
    }

    @Test
    void getTransactionById_ShouldThrowException_WhenNotFound() {
        when(transactionRepository.findByTransactionId("INVALID")).thenReturn(Optional.empty());

        assertThrows(TransactionException.class, () ->
                transactionService.getTransactionById("INVALID"));
    }

    @Test
    void getUserTransactions_ShouldReturnTransactions() {
        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));
        when(transactionRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(transactionPage);

        Page<Transaction> result = transactionService.getUserTransactions(1L, 0, 20);

        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findByUserId(eq(1L), any(Pageable.class));
    }

    @Test
    void getCardTransactions_ShouldReturnTransactions() {
        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));
        when(transactionRepository.findByCardId(eq(1L), any(Pageable.class))).thenReturn(transactionPage);

        Page<Transaction> result = transactionService.getCardTransactions(1L, 0, 20);

        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findByCardId(eq(1L), any(Pageable.class));
    }

    @Test
    void getTotalTransferredAmount_ShouldReturnTotal() {
        when(transactionRepository.findTotalWithdrawnAmount(eq(1L), any(LocalDateTime.class)))
                .thenReturn(1500.0);

        BigDecimal result = transactionService.getTotalTransferredAmount(1L, 1);

        assertEquals(new BigDecimal("1500.0"), result);
        verify(transactionRepository).findTotalWithdrawnAmount(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void getTotalTransferredAmount_ShouldReturnZero_WhenNoTransactions() {
        when(transactionRepository.findTotalWithdrawnAmount(eq(1L), any(LocalDateTime.class)))
                .thenReturn(null);

        BigDecimal result = transactionService.getTotalTransferredAmount(1L, 1);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void transferBetweenOwnCards_ShouldThrowException_WhenAmountExceedsMaxPerTransaction() {
        transferRequest.setAmount(new BigDecimal("15000.00"));

        when(validationUtil.isValidAmount(any(BigDecimal.class))).thenReturn(true);
        when(encryptionUtil.hash("1111111111111111")).thenReturn("hash-1111");
        when(encryptionUtil.hash("2222222222222222")).thenReturn("hash-2222");
        when(cardRepository.findByCardNumberHash("hash-1111")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByCardNumberHash("hash-2222")).thenReturn(Optional.of(toCard));
        when(encryptionUtil.decrypt("encrypted-cvv1")).thenReturn("123");
        when(transactionRepository.findTotalWithdrawnAmount(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0.0);

        assertThrows(TransactionException.class, () ->
                transactionService.transferBetweenOwnCards(transferRequest, 1L));
    }
}