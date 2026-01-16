package com.example.bankcards.scheduler;

import com.example.bankcards.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardExpirationScheduler {

    private final CardService cardService;


    @Scheduled(cron = "0 0 3 * * ?")
    public void checkAndUpdateExpiredCards() {
        log.info("Starting expired cards check...");
        try {
            cardService.updateExpiredCardsStatus();
            log.info("Expired cards check completed successfully");
        } catch (Exception e) {
            log.error("Error during expired cards check: {}", e.getMessage(), e);
        }
    }
}