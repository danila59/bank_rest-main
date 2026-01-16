package com.example.bankcards.service;

import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.TransactionStatus;
import com.example.bankcards.entity.enums.TransactionType;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.TransactionException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
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
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final CardService cardService;
    private final EncryptionUtil encryptionUtil;
    private final ValidationUtil validationUtil;

    @Transactional
    public Transaction transferBetweenOwnCards(TransactionDTO.TransferRequest request, Long userId) {

        if (!validationUtil.isValidAmount(request.getAmount())){
            throw new TransactionException("Invalid amount", HttpStatus.BAD_REQUEST);
        }

        Card fromCard = validateAndGetCard(request.getFromCardNumber(), userId);
        Card toCard = validateAndGetCard(request.getToCardNumber(), userId);

        String decryptedCVV = encryptionUtil.decrypt(fromCard.getCvv());
        if (!decryptedCVV.equals(request.getCvv())) {
            throw new TransactionException("Invalid CVV", HttpStatus.BAD_REQUEST, "CVV verification failed");
        }

        validateCardForTransaction(fromCard, "sender");
        validateCardForTransaction(toCard, "receiver");

        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new TransactionException("Insufficient funds", HttpStatus.BAD_REQUEST);
        }

        validateTransactionLimits(fromCard, request.getAmount());

        try {

            BigDecimal newFromBalance = fromCard.getBalance().subtract(request.getAmount());
            BigDecimal newToBalance = toCard.getBalance().add(request.getAmount());

            cardService.updateBalance(fromCard.getId(), newFromBalance);
            cardService.updateBalance(toCard.getId(), newToBalance);

            Transaction transaction = Transaction.builder()
                    .amount(request.getAmount())
                    .type(TransactionType.TRANSFER)
                    .status(TransactionStatus.COMPLETED)
                    .description(request.getDescription())
                    .fromCard(fromCard)
                    .toCard(toCard)
                    .transactionDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            Transaction savedTransaction = transactionRepository.save(transaction);

            log.info("Transfer completed: {} from card {} to card {}",
                    request.getAmount(), fromCard.getId(), toCard.getId());

            return savedTransaction;

        } catch (Exception e) {
            log.error("Transfer failed: {}", e.getMessage());
            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionException("Transaction not found"));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getUserTransactions(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        return transactionRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getCardTransactions(Long cardId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        return transactionRepository.findByCardId(cardId, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalTransferredAmount(Long cardId, int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        Double total = transactionRepository.findTotalWithdrawnAmount(cardId, fromDate);
        return total != null ? BigDecimal.valueOf(total) : BigDecimal.ZERO;
    }

    private Card validateAndGetCard(String cardNumber, Long userId) {
        String cardNumberHash = encryptionUtil.hash(cardNumber);
        Card card = cardRepository.findByCardNumberHash(cardNumberHash)
                .orElseThrow(() -> new CardOperationException("Card not found"));

        if (!card.getOwner().getId().equals(userId)) {
            throw new CardOperationException("Card does not belong to user");
        }

        return card;
    }

    private void validateCardForTransaction(Card card, String role) {
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new CardOperationException(role + " card is blocked");
        }

        if (card.getStatus() == CardStatus.EXPIRED || card.isExpired()) {
            throw new CardOperationException(role + " card is expired");
        }

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException(role + " card is not active");
        }
    }

    private void validateTransactionLimits(Card card, BigDecimal amount) {
        BigDecimal dailyLimit = new BigDecimal("5000");
        BigDecimal dailyTotal = getTotalTransferredAmount(card.getId(), 1);

        if (dailyTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new TransactionException("Daily transfer limit exceeded");
        }

        BigDecimal maxPerTransaction = new BigDecimal("10000");
        if (amount.compareTo(maxPerTransaction) > 0) {
            throw new TransactionException("Amount exceeds maximum per transaction");
        }
    }
}