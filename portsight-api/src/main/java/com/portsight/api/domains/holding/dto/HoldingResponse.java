package com.portsight.api.domains.holding.dto;

import com.portsight.api.domains.asset.dto.AssetResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class HoldingResponse {
    private UUID id;
    private UUID portfolioId;
    private UUID assetId;
    private AssetResponse asset;
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentValue;
    private BigDecimal totalInvestment;
    private BigDecimal unrealizedGain;
    private Instant createdAt;
    private Instant updatedAt;

    public HoldingResponse() {}

    // ---- Getters ----
    public UUID getId() { return id; }
    public UUID getPortfolioId() { return portfolioId; }
    public UUID getAssetId() { return assetId; }
    public AssetResponse getAsset() { return asset; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public BigDecimal getCurrentValue() { return currentValue; }
    public BigDecimal getTotalInvestment() { return totalInvestment; }
    public BigDecimal getUnrealizedGain() { return unrealizedGain; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ---- Setters ----
    public void setId(UUID id) { this.id = id; }
    public void setPortfolioId(UUID portfolioId) { this.portfolioId = portfolioId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public void setAsset(AssetResponse asset) { this.asset = asset; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    public void setTotalInvestment(BigDecimal totalInvestment) { this.totalInvestment = totalInvestment; }
    public void setUnrealizedGain(BigDecimal unrealizedGain) { this.unrealizedGain = unrealizedGain; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // ---- Builder ----
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final HoldingResponse r = new HoldingResponse();
        public Builder id(UUID id) { r.id = id; return this; }
        public Builder portfolioId(UUID portfolioId) { r.portfolioId = portfolioId; return this; }
        public Builder assetId(UUID assetId) { r.assetId = assetId; return this; }
        public Builder asset(AssetResponse asset) { r.asset = asset; return this; }
        public Builder quantity(BigDecimal quantity) { r.quantity = quantity; return this; }
        public Builder averagePrice(BigDecimal averagePrice) { r.averagePrice = averagePrice; return this; }
        public Builder currentValue(BigDecimal currentValue) { r.currentValue = currentValue; return this; }
        public Builder totalInvestment(BigDecimal totalInvestment) { r.totalInvestment = totalInvestment; return this; }
        public Builder unrealizedGain(BigDecimal unrealizedGain) { r.unrealizedGain = unrealizedGain; return this; }
        public Builder createdAt(Instant createdAt) { r.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { r.updatedAt = updatedAt; return this; }
        public HoldingResponse build() { return r; }
    }
}
