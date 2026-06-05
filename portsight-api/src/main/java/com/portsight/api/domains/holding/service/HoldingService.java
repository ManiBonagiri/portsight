package com.portsight.api.domains.holding.service;

import com.portsight.api.domains.asset.dto.AssetResponse;
import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.mapper.AssetMapper;
import com.portsight.api.domains.asset.repository.AssetRepository;
import com.portsight.api.domains.holding.dto.AddHoldingRequest;
import com.portsight.api.domains.holding.dto.HoldingResponse;
import com.portsight.api.domains.holding.entity.Holding;
import com.portsight.api.domains.holding.mapper.HoldingMapper;
import com.portsight.api.domains.holding.repository.HoldingRepository;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HoldingService {

    private static final Logger log = LoggerFactory.getLogger(HoldingService.class);

    private final HoldingRepository holdingRepository;
    private final HoldingMapper holdingMapper;
    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

    public HoldingService(HoldingRepository holdingRepository,
                          HoldingMapper holdingMapper,
                          PortfolioRepository portfolioRepository,
                          AssetRepository assetRepository,
                          AssetMapper assetMapper) {
        this.holdingRepository = holdingRepository;
        this.holdingMapper = holdingMapper;
        this.portfolioRepository = portfolioRepository;
        this.assetRepository = assetRepository;
        this.assetMapper = assetMapper;
    }

    @Transactional
    public HoldingResponse addHolding(UUID userId, UUID portfolioId, AddHoldingRequest request) {
        log.info("Adding holding to portfolio: {} for asset: {}", portfolioId, request.getAssetId());

        verifyPortfolioOwnership(userId, portfolioId);

        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + request.getAssetId()));

        Holding holding = holdingRepository.findByPortfolioIdAndAssetId(portfolioId, request.getAssetId())
                .orElse(Holding.builder()
                        .portfolioId(portfolioId)
                        .assetId(request.getAssetId())
                        .quantity(BigDecimal.ZERO)
                        .averagePrice(BigDecimal.ZERO)
                        .build());

        BigDecimal oldTotal = holding.getQuantity().multiply(holding.getAveragePrice());
        BigDecimal newTotal = request.getQuantity().multiply(request.getPrice());
        BigDecimal totalQty = holding.getQuantity().add(request.getQuantity());

        BigDecimal newAvgPrice = oldTotal.add(newTotal).divide(totalQty, 4, RoundingMode.HALF_UP);

        holding.setQuantity(totalQty);
        holding.setAveragePrice(newAvgPrice);

        Holding savedHolding = holdingRepository.save(holding);
        return enrichHoldingResponse(savedHolding, asset);
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> getPortfolioHoldings(UUID userId, UUID portfolioId) {
        log.info("Fetching holdings for portfolio: {}", portfolioId);

        verifyPortfolioOwnership(userId, portfolioId);

        return holdingRepository.findByPortfolioId(portfolioId).stream()
                .map(holding -> {
                    Asset asset = assetRepository.findById(holding.getAssetId()).orElse(null);
                    return enrichHoldingResponse(holding, asset);
                })
                .collect(Collectors.toList());
    }

    private void verifyPortfolioOwnership(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
        if (!portfolio.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Portfolio not found: " + portfolioId);
        }
    }

    private HoldingResponse enrichHoldingResponse(Holding holding, Asset asset) {
        HoldingResponse response = holdingMapper.toResponse(holding);
        if (asset != null) {
            response.setAsset(assetMapper.toResponse(asset));

            BigDecimal currentPrice = asset.getCurrentPrice() != null
                    ? asset.getCurrentPrice()
                    : holding.getAveragePrice();
            BigDecimal currentValue = holding.getQuantity().multiply(currentPrice);
            BigDecimal totalInvestment = holding.getQuantity().multiply(holding.getAveragePrice());
            BigDecimal unrealizedGain = currentValue.subtract(totalInvestment);

            response.setCurrentValue(currentValue);
            response.setTotalInvestment(totalInvestment);
            response.setUnrealizedGain(unrealizedGain);
        }
        return response;
    }
}
