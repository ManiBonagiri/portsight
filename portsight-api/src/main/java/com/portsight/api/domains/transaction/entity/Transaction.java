package com.portsight.api.domains.transaction.entity;

import com.portsight.api.domains.transaction.enums.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "asset_id") // Nullable for DEPOSIT/WITHDRAWAL cash transactions
    private UUID assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED"; // COMPLETED, REVERSED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ---- Constructors ----
    public Transaction() {}

    public Transaction(UUID id, UUID portfolioId, UUID assetId, TransactionType type,
                       BigDecimal quantity, BigDecimal price, String status,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.assetId = assetId;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ---- Getters ----
    public UUID getId() { return id; }
    public UUID getPortfolioId() { return portfolioId; }
    public UUID getAssetId() { return assetId; }
    public TransactionType getType() { return type; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ---- Setters ----
    public void setId(UUID id) { this.id = id; }
    public void setPortfolioId(UUID portfolioId) { this.portfolioId = portfolioId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public void setType(TransactionType type) { this.type = type; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // ---- Builder ----
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID portfolioId;
        private UUID assetId;
        private TransactionType type;
        private BigDecimal quantity;
        private BigDecimal price;
        private String status = "COMPLETED";
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder portfolioId(UUID portfolioId) { this.portfolioId = portfolioId; return this; }
        public Builder assetId(UUID assetId) { this.assetId = assetId; return this; }
        public Builder type(TransactionType type) { this.type = type; return this; }
        public Builder quantity(BigDecimal quantity) { this.quantity = quantity; return this; }
        public Builder price(BigDecimal price) { this.price = price; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Transaction build() {
            return new Transaction(id, portfolioId, assetId, type, quantity, price, status, createdAt, updatedAt);
        }
    }
}
