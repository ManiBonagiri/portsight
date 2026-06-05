package com.portsight.api.domains.transaction.service;

import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.mapper.AssetMapper;
import com.portsight.api.domains.asset.repository.AssetRepository;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.domains.transaction.dto.TransactionRequest;
import com.portsight.api.domains.transaction.dto.TransactionResponse;
import com.portsight.api.domains.transaction.entity.Transaction;
import com.portsight.api.domains.transaction.mapper.TransactionMapper;
import com.portsight.api.domains.transaction.repository.TransactionRepository;
import com.portsight.api.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionMapper transactionMapper,
                              PortfolioRepository portfolioRepository,
                              AssetRepository assetRepository,
                              AssetMapper assetMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.portfolioRepository = portfolioRepository;
        this.assetRepository = assetRepository;
        this.assetMapper = assetMapper;
    }

    @Transactional
    public TransactionResponse recordTransaction(UUID userId, TransactionRequest request) {
        log.info("Recording transaction for portfolio: {}", request.getPortfolioId());

        verifyPortfolioOwnership(userId, request.getPortfolioId());

        if (request.getAssetId() != null) {
            assetRepository.findById(request.getAssetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + request.getAssetId()));
        }

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setStatus("COMPLETED");
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // TODO: Publish Kafka event 'transaction-recorded' to update holdings/cash asynchronously

        return enrichTransactionResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getPortfolioTransactions(UUID userId, UUID portfolioId) {
        log.info("Fetching transactions for portfolio: {}", portfolioId);
        
        verifyPortfolioOwnership(userId, portfolioId);

        return transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId).stream()
                .map(this::enrichTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse reverseTransaction(UUID userId, UUID transactionId) {
        log.info("Reversing transaction: {}", transactionId);
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
                
        verifyPortfolioOwnership(userId, transaction.getPortfolioId());

        if ("REVERSED".equals(transaction.getStatus())) {
            throw new IllegalStateException("Transaction is already reversed");
        }

        transaction.setStatus("REVERSED");
        Transaction updatedTransaction = transactionRepository.save(transaction);
        
        // TODO: Publish Kafka event 'transaction-reversed'
        
        return enrichTransactionResponse(updatedTransaction);
    }

    private void verifyPortfolioOwnership(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
        if (!portfolio.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Portfolio not found: " + portfolioId);
        }
    }

    private TransactionResponse enrichTransactionResponse(Transaction transaction) {
        TransactionResponse response = transactionMapper.toResponse(transaction);
        if (transaction.getAssetId() != null) {
            Asset asset = assetRepository.findById(transaction.getAssetId()).orElse(null);
            if (asset != null) {
                response.setAsset(assetMapper.toResponse(asset));
            }
        }
        
        BigDecimal total = transaction.getQuantity().multiply(transaction.getPrice());
        response.setTotalAmount(total);
        
        return response;
    }
}
