package com.example.bankcards.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;

@Component
public class CardNumberGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final String[] BINS = {
            "414947",
            "524154",
            "377765",
            "601122"
    };

    public String generateCardNumber(String bin) {
        if (bin == null || bin.length() != 6 || !bin.matches("\\d{6}")) {

            bin = BINS[random.nextInt(BINS.length)];
        }

        StringBuilder cardNumber = new StringBuilder(bin);

        for (int i = 0; i < 9; i++) {
            cardNumber.append(random.nextInt(10));
        }

        cardNumber.append(calculateLuhnCheckDigit(cardNumber.toString()));

        return cardNumber.toString();
    }

    public String generateCVV() {
        return String.format("%03d", random.nextInt(1000));
    }

    public LocalDate generateExpiryDate() {
        int years = 3 + random.nextInt(3);
        return LocalDate.now()
                .plusYears(years)
                .withDayOfMonth(1)
                .plusMonths(random.nextInt(12));
    }

    public boolean isValidExpiryDate(LocalDate expiryDate) {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusYears(10);

        return expiryDate != null &&
                expiryDate.isAfter(today.minusDays(1)) &&
                expiryDate.isBefore(maxDate);
    }

    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        return (10 - (sum % 10)) % 10;
    }

    public boolean validateLuhn(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 13 || cardNumber.length() > 19) {
            return false;
        }

        try {
            int checkDigit = Character.getNumericValue(cardNumber.charAt(cardNumber.length() - 1));
            String numberWithoutCheckDigit = cardNumber.substring(0, cardNumber.length() - 1);

            return checkDigit == calculateLuhnCheckDigit(numberWithoutCheckDigit);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
