package com.example.bankcards.util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CardMaskingUtil {

    @Value("${app.card.mask-pattern:**** **** **** %s}")
    private String maskPattern;

    @Value("${app.card.number-length:16}")
    private int cardNumberLength;

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\d{16}");

    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != cardNumberLength) {
            throw new IllegalArgumentException("Invalid card number length");
        }

        if (!CARD_NUMBER_PATTERN.matcher(cardNumber).matches()) {
            throw new IllegalArgumentException("Invalid card number format");
        }

        String lastFourDigits = cardNumber.substring(cardNumber.length() - 4);
        return String.format(maskPattern, lastFourDigits);
    }

    public String getLastFourDigits(String maskedCardNumber) {
        if (maskedCardNumber == null || maskedCardNumber.length() < 4) {
            return "";
        }
        return maskedCardNumber.substring(maskedCardNumber.length() - 4);
    }

    public String validateAndMask(String cardNumber) {
        String cleanNumber = cardNumber.replaceAll("\\s+", "");

        if (cleanNumber.length() != cardNumberLength) {
            throw new IllegalArgumentException("Card number must be " + cardNumberLength + " digits");
        }

        if (!cleanNumber.matches("\\d{" + cardNumberLength + "}")) {
            throw new IllegalArgumentException("Card number must contain only digits");
        }

        return maskCardNumber(cleanNumber);
    }
}
