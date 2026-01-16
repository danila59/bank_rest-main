package com.example.bankcards.mapper;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

    public CardDTO.Response toResponse(Card card){
        if (card == null) return null;

        return CardDTO.Response.builder()
                .id(card.getId())
                .maskedNumber(card.getMaskedNumber())
                .ownerName(card.getOwnerName())
                .expiryDate(card.getExpiryDate().toString())
                .status(card.getStatus())
                .balance(card.getBalance())
                .expired(card.isExpired())
                .createdAt(card.getCreatedAt())
                .userId(card.getOwner().getId())
                .username(card.getOwner().getUsername())
                .build();
    }
}
