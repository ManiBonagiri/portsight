package com.portsight.api.domains.transaction.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class TransactionReversalRequest {
    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;

    public TransactionReversalRequest() {}

    public TransactionReversalRequest(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }
}
