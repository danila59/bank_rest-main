package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByCardNumberHash(String cardNumberHash);

    List<Card> findByOwnerId(Long ownerId);

    Page<Card> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.expiryDate < :date")
    List<Card> findExpiredCards(@Param("date") LocalDate date);

    @Query("SELECT c FROM Card c WHERE c.owner.id = :userId AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:lastFour IS NULL OR c.maskedNumber LIKE %:lastFour%)")
    Page<Card> findCardsWithFilters(@Param("userId") Long userId,
                                    @Param("status") CardStatus status,
                                    @Param("lastFour") String lastFour,
                                    Pageable pageable);

    boolean existsByCardNumberHash(String cardNumberHash);
}
