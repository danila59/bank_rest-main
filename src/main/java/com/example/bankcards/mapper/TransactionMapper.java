package com.example.bankcards.mapper;

import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionDTO.Response toResponse(Transaction transaction) {
        if (transaction == null) return null;

        return TransactionDTO.Response.builder()
                .transactionId(transaction.getTransactionId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .description(transaction.getDescription())
                .fromCardMasked(transaction.getFromCard() != null ?
                        transaction.getFromCard().getMaskedNumber() : null)
                .toCardMasked(transaction.getToCard().getMaskedNumber())
                .transactionDate(transaction.getTransactionDate())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
