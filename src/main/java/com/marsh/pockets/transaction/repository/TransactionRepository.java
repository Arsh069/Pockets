package com.marsh.pockets.transaction.repository;

import com.marsh.pockets.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Transaction t WHERE (:pocketId IS NULL OR t.pocketId = :pocketId) AND (:userId IS NULL OR t.userId = :userId)")
    List<Transaction> findAllFiltered(@Param("pocketId") Long pocketId, @Param("userId") Long userId);
}
