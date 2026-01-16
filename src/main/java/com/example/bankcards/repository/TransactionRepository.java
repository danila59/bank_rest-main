package com.example.bankcards.repository;

import com.example.bankcards.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    @Query("SELECT t FROM Transaction t WHERE t.fromCard.id = :cardId OR t.toCard.id = :cardId " +
            "ORDER BY t.transactionDate DESC")
    Page<Transaction> findByCardId(@Param("cardId") Long cardId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.fromCard.owner.id = :userId OR t.toCard.owner.id = :userId " +
            "ORDER BY t.transactionDate DESC")
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE " +
            "t.fromCard.id = :cardId AND t.status = 'COMPLETED' AND t.transactionDate >= :date")
    Double findTotalWithdrawnAmount(@Param("cardId") Long cardId,
                                    @Param("date") LocalDateTime date);
}