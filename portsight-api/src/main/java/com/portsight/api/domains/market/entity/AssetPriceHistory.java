package com.portsight.api.domains.market.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "asset_price_history", uniqueConstraints = {@UniqueConstraint(columnNames = {"asset_id", "record_date"})})
public class AssetPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "closing_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal closingPrice;

    // ---- Constructors ----
    public AssetPriceHistory() {}

    public AssetPriceHistory(UUID id, UUID assetId, LocalDate recordDate, BigDecimal closingPrice) {
        this.id = id;
        this.assetId = assetId;
        this.recordDate = recordDate;
        this.closingPrice = closingPrice;
    }

    // ---- Getters ----
    public UUID getId() { return id; }
    public UUID getAssetId() { return assetId; }
    public LocalDate getRecordDate() { return recordDate; }
    public BigDecimal getClosingPrice() { return closingPrice; }

    // ---- Setters ----
    public void setId(UUID id) { this.id = id; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public void setClosingPrice(BigDecimal closingPrice) { this.closingPrice = closingPrice; }

    // ---- Builder ----
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID assetId;
        private LocalDate recordDate;
        private BigDecimal closingPrice;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder assetId(UUID assetId) { this.assetId = assetId; return this; }
        public Builder recordDate(LocalDate recordDate) { this.recordDate = recordDate; return this; }
        public Builder closingPrice(BigDecimal closingPrice) { this.closingPrice = closingPrice; return this; }

        public AssetPriceHistory build() {
            return new AssetPriceHistory(id, assetId, recordDate, closingPrice);
        }
    }
}
