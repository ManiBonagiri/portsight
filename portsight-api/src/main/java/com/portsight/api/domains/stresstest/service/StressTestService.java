package com.portsight.api.domains.stresstest.service;

import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.repository.AssetRepository;
import com.portsight.api.domains.holding.entity.Holding;
import com.portsight.api.domains.holding.repository.HoldingRepository;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.domains.stresstest.dto.StressTestRequest;
import com.portsight.api.domains.stresstest.dto.StressTestResponse;
import com.portsight.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StressTestService {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final AssetRepository assetRepository;

    @Transactional(readOnly = true)
    public StressTestResponse runStressTest(UUID userId, StressTestRequest request) {
        log.info("Running stress test for portfolio: {} with scenario: {}", request.getPortfolioId(), request.getScenario());
        
        verifyPortfolioOwnership(userId, request.getPortfolioId());

        List<Holding> holdings = holdingRepository.findByPortfolioId(request.getPortfolioId());
        
        BigDecimal preStressValue = BigDecimal.ZERO;
        BigDecimal postStressValue = BigDecimal.ZERO;

        for (Holding holding : holdings) {
            Asset asset = assetRepository.findById(holding.getAssetId()).orElse(null);
            BigDecimal currentPrice = (asset != null && asset.getCurrentPrice() != null) 
                    ? asset.getCurrentPrice() 
                    : holding.getAveragePrice();
                    
            BigDecimal holdingValue = holding.getQuantity().multiply(currentPrice);
            preStressValue = preStressValue.add(holdingValue);
            
            // Apply shock
            BigDecimal shockedPrice = applyScenarioShock(currentPrice, asset, request.getScenario());
            postStressValue = postStressValue.add(holding.getQuantity().multiply(shockedPrice));
        }

        BigDecimal impactAmount = postStressValue.subtract(preStressValue);
        BigDecimal impactPercent = BigDecimal.ZERO;
        
        if (preStressValue.compareTo(BigDecimal.ZERO) > 0) {
            impactPercent = impactAmount.divide(preStressValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        return StressTestResponse.builder()
                .scenario(request.getScenario())
                .preStressValue(preStressValue)
                .postStressValue(postStressValue)
                .impactAmount(impactAmount)
                .impactPercent(impactPercent)
                .build();
    }

    private BigDecimal applyScenarioShock(BigDecimal price, Asset asset, String scenario) {
        if (price == null) return BigDecimal.ZERO;
        if (asset == null) return price; // Cannot apply specific shocks if asset unknown
        
        double shockFactor = 1.0;
        
        switch (scenario.toUpperCase()) {
            case "MARKET_CRASH_20":
                shockFactor = 0.80; // 20% drop across the board
                break;
            case "MARKET_CRASH_30":
                shockFactor = 0.70; // 30% drop
                break;
            case "TECH_CRASH_15":
                if ("Technology".equalsIgnoreCase(asset.getSector())) {
                    shockFactor = 0.85;
                } else {
                    shockFactor = 0.95; // Slight ripple effect
                }
                break;
            case "RATE_HIKE_2":
                // Bonds drop when rates rise, stocks might see minor correction
                if (asset.getAssetType() != null && "BOND".equals(asset.getAssetType().name())) {
                    shockFactor = 0.90;
                } else {
                    shockFactor = 0.98;
                }
                break;
            default:
                shockFactor = 1.0;
        }

        return price.multiply(BigDecimal.valueOf(shockFactor)).setScale(4, RoundingMode.HALF_UP);
    }

    private void verifyPortfolioOwnership(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
        if (!portfolio.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Portfolio not found: " + portfolioId);
        }
    }
}
