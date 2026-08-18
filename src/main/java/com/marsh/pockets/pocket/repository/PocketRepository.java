package com.marsh.pockets.pocket.repository;

import com.marsh.pockets.pocket.entity.Pocket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PocketRepository extends JpaRepository<Pocket, Long> {

    List<Pocket> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE Pocket p SET p.currentBalance = p.currentBalance - :amount WHERE p.id = :id AND p.currentBalance >= :amount")
    int deductBalanceAtomic(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
