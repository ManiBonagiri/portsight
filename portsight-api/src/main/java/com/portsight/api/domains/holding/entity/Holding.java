package com.portsight.api.domains.holding.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "holdings", uniqueConstraints = {@UniqueConstraint(columnNames = {"portfolio_id", "asset_id"})})
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "average_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal averagePrice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ---- Constructors ----
    public Holding() {}

    public Holding(UUID id, UUID portfolioId, UUID assetId, BigDecimal quantity,
                   BigDecimal averagePrice, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.assetId = assetId;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ---- Getters ----
    public UUID getId() { return id; }
    public UUID getPortfolioId() { return portfolioId; }
    public UUID getAssetId() { return assetId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ---- Setters ----
    public void setId(UUID id) { this.id = id; }
    public void setPortfolioId(UUID portfolioId) { this.portfolioId = portfolioId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // ---- Builder ----
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID portfolioId;
        private UUID assetId;
        private BigDecimal quantity;
        private BigDecimal averagePrice;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder portfolioId(UUID portfolioId) { this.portfolioId = portfolioId; return this; }
        public Builder assetId(UUID assetId) { this.assetId = assetId; return this; }
        public Builder quantity(BigDecimal quantity) { this.quantity = quantity; return this; }
        public Builder averagePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; return this; }

        public Holding build() {
            Holding h = new Holding();
            h.id = this.id;
            h.portfolioId = this.portfolioId;
            h.assetId = this.assetId;
            h.quantity = this.quantity;
            h.averagePrice = this.averagePrice;
            return h;
        }
    }
}
