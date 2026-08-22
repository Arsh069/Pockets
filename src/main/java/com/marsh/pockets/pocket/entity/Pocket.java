package com.marsh.pockets.pocket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "pockets",
    check = @CheckConstraint(name = "chk_pocket_balance_non_negative", constraint = "current_balance >= 0")
)
public class Pocket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "monthly_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal monthlyLimit;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Column(name = "manual_current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal manualCurrentBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_reset_at")
    private Instant lastResetAt;

    public Pocket() {
    }

    public Pocket(Long userId, String name, BigDecimal monthlyLimit) {
        this.userId = userId;
        this.name = name;
        this.monthlyLimit = monthlyLimit;
        this.currentBalance = monthlyLimit;
        this.manualCurrentBalance = monthlyLimit;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.currentBalance == null) {
            this.currentBalance = this.monthlyLimit != null ? this.monthlyLimit : BigDecimal.ZERO;
        }
        if (this.manualCurrentBalance == null) {
            this.manualCurrentBalance = this.currentBalance;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getManualCurrentBalance() {
        return manualCurrentBalance;
    }

    public void setManualCurrentBalance(BigDecimal manualCurrentBalance) {
        this.manualCurrentBalance = manualCurrentBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastResetAt() {
        return lastResetAt;
    }

    public void setLastResetAt(Instant lastResetAt) {
        this.lastResetAt = lastResetAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
