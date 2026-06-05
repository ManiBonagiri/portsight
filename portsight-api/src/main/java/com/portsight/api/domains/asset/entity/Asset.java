package com.portsight.api.domains.asset.entity;

import com.portsight.api.domains.asset.enums.AssetType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assets", uniqueConstraints = {@UniqueConstraint(columnNames = {"ticker"})})
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String sector;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private AssetType assetType;

    @Column(name = "current_price", precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ---- Constructors ----
    public Asset() {}

    public Asset(UUID id, String ticker, String name, String sector, AssetType assetType,
                 BigDecimal currentPrice, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ticker = ticker;
        this.name = name;
        this.sector = sector;
        this.assetType = assetType;
        this.currentPrice = currentPrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ---- Getters ----
    public UUID getId() { return id; }
    public String getTicker() { return ticker; }
    public String getName() { return name; }
    public String getSector() { return sector; }
    public AssetType getAssetType() { return assetType; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ---- Setters ----
    public void setId(UUID id) { this.id = id; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setName(String name) { this.name = name; }
    public void setSector(String sector) { this.sector = sector; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // ---- Builder ----
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String ticker;
        private String name;
        private String sector;
        private AssetType assetType;
        private BigDecimal currentPrice;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder ticker(String ticker) { this.ticker = ticker; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder sector(String sector) { this.sector = sector; return this; }
        public Builder assetType(AssetType assetType) { this.assetType = assetType; return this; }
        public Builder currentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Asset build() {
            return new Asset(id, ticker, name, sector, assetType, currentPrice, createdAt, updatedAt);
        }
    }

    @Override
    public String toString() {
        return "Asset{id=" + id + ", ticker='" + ticker + "', assetType=" + assetType + ", currentPrice=" + currentPrice + "}";
    }
}
