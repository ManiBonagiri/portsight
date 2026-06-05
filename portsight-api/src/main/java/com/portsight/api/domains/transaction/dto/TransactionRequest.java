package com.portsight.api.domains.transaction.dto;

import com.portsight.api.domains.transaction.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionRequest {
    @NotNull(message = "Portfolio ID is required")
    private UUID portfolioId;

    private UUID assetId;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;

    public TransactionRequest() {}

    public TransactionRequest(UUID portfolioId, UUID assetId, TransactionType type,
                              BigDecimal quantity, BigDecimal price) {
        this.portfolioId = portfolioId;
        this.assetId = assetId;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
    }

    public UUID getPortfolioId() { return portfolioId; }
    public UUID getAssetId() { return assetId; }
    public TransactionType getType() { return type; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }

    public void setPortfolioId(UUID portfolioId) { this.portfolioId = portfolioId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public void setType(TransactionType type) { this.type = type; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
