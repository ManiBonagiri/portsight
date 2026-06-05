package com.portsight.api.domains.transaction;

import com.portsight.api.domains.transaction.dto.TransactionRequest;
import com.portsight.api.domains.transaction.dto.TransactionResponse;
import com.portsight.api.domains.transaction.dto.TransactionReversalRequest;
import com.portsight.api.domains.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    private UUID getUserId(String principal) {
        return UUID.fromString(principal);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> processTransaction(
            @AuthenticationPrincipal String principal,
            @Valid @RequestBody TransactionRequest request) {
        
        UUID userId = getUserId(principal);
        TransactionResponse response = transactionService.recordTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", response));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getTransactionHistory(
            @AuthenticationPrincipal String principal,
            @RequestParam UUID portfolioId) {
        
        UUID userId = getUserId(principal);
        List<TransactionResponse> response = transactionService.getPortfolioTransactions(userId, portfolioId);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @PostMapping("/reversal")
    public ResponseEntity<Map<String, Object>> reverseTransaction(
            @AuthenticationPrincipal String principal,
            @Valid @RequestBody TransactionReversalRequest request) {
        
        UUID userId = getUserId(principal);
        TransactionResponse response = transactionService.reverseTransaction(userId, request.getTransactionId());
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }
}
