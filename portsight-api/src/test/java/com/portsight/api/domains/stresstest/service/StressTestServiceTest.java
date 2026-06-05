package com.portsight.api.domains.stresstest.service;

import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.enums.AssetType;
import com.portsight.api.domains.asset.repository.AssetRepository;
import com.portsight.api.domains.holding.entity.Holding;
import com.portsight.api.domains.holding.repository.HoldingRepository;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.domains.stresstest.dto.StressTestRequest;
import com.portsight.api.domains.stresstest.dto.StressTestResponse;
import com.portsight.api.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StressTestService Tests")
class StressTestServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private StressTestService stressTestService;

    // Shared fixtures
    private UUID userId;
    private UUID portfolioId;
    private UUID assetId;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        assetId = UUID.randomUUID();

        portfolio = new Portfolio();
        portfolio.setId(portfolioId);
        portfolio.setUserId(userId);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Holding holding(UUID assetId, BigDecimal qty, BigDecimal avgPrice) {
        return Holding.builder()
                .id(UUID.randomUUID())
                .portfolioId(portfolioId)
                .assetId(assetId)
                .quantity(qty)
                .averagePrice(avgPrice)
                .build();
    }

    private Asset asset(UUID id, String sector, AssetType type, BigDecimal currentPrice) {
        Asset a = new Asset();
        a.setId(id);
        a.setSector(sector);
        a.setAssetType(type);
        a.setCurrentPrice(currentPrice);
        return a;
    }

    private StressTestRequest request(String scenario) {
        return StressTestRequest.builder()
                .portfolioId(portfolioId)
                .scenario(scenario)
                .build();
    }

    // ── ownership & guard tests ──────────────────────────────────────────────
    @Nested
    @DisplayName("Ownership & Guard Tests")
    class OwnershipTests {

        @Test
        @DisplayName("shouldThrowWhenPortfolioNotFound")
        void shouldThrowWhenPortfolioNotFound() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stressTestService.runStressTest(userId, request("MARKET_CRASH_20")))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(holdingRepository, never()).findByPortfolioId(any());
        }

        @Test
        @DisplayName("shouldThrowWhenUserDoesNotOwnPortfolio")
        void shouldThrowWhenUserDoesNotOwnPortfolio() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> stressTestService.runStressTest(UUID.randomUUID(), request("MARKET_CRASH_20")))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(holdingRepository, never()).findByPortfolioId(any());
        }

        @Test
        @DisplayName("shouldReturnZeroValuesWhenPortfolioHasNoHoldings")
        void shouldReturnZeroValuesWhenPortfolioHasNoHoldings() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(Collections.emptyList());

            StressTestResponse result = stressTestService.runStressTest(userId, request("MARKET_CRASH_20"));

            assertThat(result.getPreStressValue()).isEqualByComparingTo("0");
            assertThat(result.getPostStressValue()).isEqualByComparingTo("0");
            assertThat(result.getImpactAmount()).isEqualByComparingTo("0");
            assertThat(result.getImpactPercent()).isEqualByComparingTo("0");
        }
    }

    // ── MARKET_CRASH_20 — 20% drop across all assets ─────────────────────────
    @Nested
    @DisplayName("MARKET_CRASH_20 Scenario")
    class MarketCrash20Tests {

        @Test
        @DisplayName("shouldApply20PercentDropToAllAssets")
        void shouldApply20PercentDropToAllAssets() {
            // 100 shares @ 100 → preStress = 10000
            // shocked price = 100 × 0.80 = 80 → postStress = 8000
            Asset stock = asset(assetId, "Technology", AssetType.STOCK, new BigDecimal("100.00"));
            Holding h = holding(assetId, new BigDecimal("100"), new BigDecimal("100.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(h));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(stock));

            StressTestResponse result = stressTestService.runStressTest(userId, request("MARKET_CRASH_20"));

            assertThat(result.getScenario()).isEqualTo("MARKET_CRASH_20");
            assertThat(result.getPreStressValue()).isEqualByComparingTo("10000.00");
            assertThat(result.getPostStressValue()).isEqualByComparingTo("8000.00");
            assertThat(result.getImpactAmount()).isEqualByComparingTo("-2000.00");
            // impactPercent = -2000/10000 × 100 = -20.0000
            assertThat(result.getImpactPercent()).isEqualByComparingTo("-20.0000");
        }

        @Test
        @DisplayName("shouldApplyCrash20ToBothTechAndNonTechAssets")
        void shouldApplyCrash20ToBothTechAndNonTechAssets() {
            UUID bankId = UUID.randomUUID();
            Asset tech = asset(assetId, "Technology", AssetType.STOCK, new BigDecimal("100.00"));
            Asset bank = asset(bankId, "Banking", AssetType.STOCK, new BigDecimal("200.00"));
            Holding hTech = holding(assetId, new BigDecimal("50"), new BigDecimal("90.00"));
            Holding hBank = holding(bankId, new BigDecimal("25"), new BigDecimal("180.00"));

            // preStress = 50×100 + 25×200 = 5000 + 5000 = 10000
            // postStress = 50×80 + 25×160 = 4000 + 4000 = 8000

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(hTech, hBank));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(tech));
            when(assetRepository.findById(bankId)).thenReturn(Optional.of(bank));

            StressTestResponse result = stressTestService.runStressTest(userId, request("MARKET_CRASH_20"));

            assertThat(result.getPreStressValue()).isEqualByComparingTo("10000.00");
            assertThat(result.getPostStressValue()).isEqualByComparingTo("8000.00");
            assertThat(result.getImpactPercent()).isEqualByComparingTo("-20.0000");
        }
    }

    // ── MARKET_CRASH_30 — 30% drop ────────────────────────────────────────────
    @Nested
    @DisplayName("MARKET_CRASH_30 Scenario")
    class MarketCrash30Tests {

        @Test
        @DisplayName("shouldApply30PercentDropToAllAssets")
        void shouldApply30PercentDropToAllAssets() {
            // 100 shares @ 100 → pre = 10000, shocked = 100×0.70 = 70 → post = 7000
            Asset stock = asset(assetId, "Energy", AssetType.STOCK, new BigDecimal("100.00"));
            Holding h = holding(assetId, new BigDecimal("100"), new BigDecimal("100.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(h));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(stock));

            StressTestResponse result = stressTestService.runStressTest(userId, request("MARKET_CRASH_30"));

            assertThat(result.getPostStressValue()).isEqualByComparingTo("7000.00");
            assertThat(result.getImpactAmount()).isEqualByComparingTo("-3000.00");
            assertThat(result.getImpactPercent()).isEqualByComparingTo("-30.0000");
        }
    }

    // ── TECH_CRASH_15 — sector-specific shock ─────────────────────────────────
    @Nested
    @DisplayName("TECH_CRASH_15 Scenario")
    class TechCrash15Tests {

        @Test
        @DisplayName("shouldApply15PercentDropToTechnologySectorOnly")
        void shouldApply15PercentDropToTechnologySectorOnly() {
            // Tech: 100 shares @ 100 → shocked = 85
            Asset tech = asset(assetId, "Technology", AssetType.STOCK, new BigDecimal("100.00"));
            Holding h = holding(assetId, new BigDecimal("100"), new BigDecimal("100.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(h));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(tech));

            StressTestResponse result = stressTestService.runStressTest(userId, request("TECH_CRASH_15"));

            assertThat(result.getPreStressValue()).isEqualByComparingTo("10000.00");
            assertThat(result.getPostStressValue()).isEqualByComparingTo("8500.00");
            assertThat(result.getImpactPercent()).isEqualByComparingTo("-15.0000");
        }

        @Test
        @DisplayName("shouldApplyRippleEffectToNonTechnologySectors")
        void shouldApplyRippleEffectToNonTechnologySectors() {
            // Non-tech gets 0.95 ripple (5% drop)
            Asset bank = asset(assetId, "Banking", AssetType.STOCK, new BigDecimal("100.00"));
            Holding h = holding(assetId, new BigDecimal("100"), new BigDecimal("100.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(h));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(bank));

            StressTestResponse result = stressTestService.runStressTest(userId, request("TECH_CRASH_15"));

            assertThat(result.getPostStressValue()).isEqualByComparingTo("9500.00");
            assertThat(result.getImpactPercent()).isEqualByComparingTo("-5.0000");
        }

        @Test
        @DisplayName("shouldApplyDifferentShocksToMixedPortfolio")
        void shouldApplyDifferentShocksToMixedPortfolio() {
            UUID bankId = UUID.randomUUID();
            Asset tech = asset(assetId, "Technology", AssetType.STOCK, new BigDecimal("100.00"));
            Asset bank = asset(bankId, "Banking", AssetType.STOCK, new BigDecimal("100.00"));
            Holding hTech = holding(assetId, new BigDecimal("100"), new BigDecimal("90.00"));
            Holding hBank = holding(bankId, new BigDecimal("100"), new BigDecimal("90.00"));

            // preStress = 100×100 + 100×100 = 20000
            // postStress = 100×85 + 100×95 = 8500 + 9500 = 18000

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(hTech, hBank));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(tech));
            when(assetRepository.findById(bankId)).thenReturn(Optional.of(bank));

            StressTestResponse result = stressTestService.runStressTest(userId, request("TECH_CRASH_15"));

            assertThat(result.getPreStressValue()).isEqualByComparingTo("20000.00");
            assertThat(result.getPostStressValue()).isEqualByComparingTo("18000.00");
            assertThat(result.getImpactAmount()).isEqualByComparingTo("-2000.00");
        }
    }

    // ── RATE_HIKE_2 — bond vs stock differentiation ───────────────────────────
    @Nested
    @DisplayName("RATE_HIKE_2 Scenario")
    class RateHike2Tests {

        @Test
        @DisplayName("shouldApply10PercentDropToBonds")
        void shouldApply10PercentDropToBonds() {
            // BOND: 100 shares @ 100 → shocked = 90
            Asset bond = asset(assetId, "Debt", AssetType.BOND, new BigDecimal("100.00"));
            Holding h = holding(assetId, new BigDecimal("100"), new BigDecimal("100.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(h));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(bond));

            StressTestResponse result = stressTestService.runStressTest(userId, request("RATE_HIKE_2"));

            assertThat(result.getPostStressValue()).isEqualByComparingTo("9000.00");
            assertThat(result.getImpactPercent()).isEqualByComparingTo("-10.0000");
        }

        @Test
        @DisplayName("shouldApply2PercentDropToNonBondAssets")
        void shouldApply2PercentDropToNonBondAssets() {
            // STOCK: 100 shares @ 100 → shocked = 98 (0.98 factor)
            Asset stock = asset(assetId, "Technology", AssetType.STOCK, new BigDecimal("100.00"));
            Holding h = holding(assetId, new BigDecimal("100"), new BigDecimal("100.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(h));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(stock));

            StressTestResponse result = stressTestService.runStressTest(userId, request("RATE_HIKE_2"));

            assertThat(result.getPostStressValue()).isEqualByComparingTo("9800.00");
            assertThat(result.getImpactPercent()).isEqualByComparingTo("-2.0000");
        }
    }

    // ── Default / unknown scenario ─────────────────────────────────────────────
    @Nested
    @DisplayName("Default & Edge Case Scenarios")
    class DefaultScenarioTests {

        @Test
        @DisplayName("shouldApplyNoShockForUnknownScenario")
        void shouldApplyNoShockForUnknownScenario() {
            // shockFactor = 1.0 → postStress == preStress
            Asset stock = asset(assetId, "Healthcare", AssetType.STOCK, new BigDecimal("200.00"));
            Holding h = holding(assetId, new BigDecimal("50"), new BigDecimal("190.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(h));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(stock));

            StressTestResponse result = stressTestService.runStressTest(userId, request("UNKNOWN_SCENARIO"));

            assertThat(result.getPreStressValue()).isEqualByComparingTo(result.getPostStressValue());
            assertThat(result.getImpactAmount()).isEqualByComparingTo("0");
            assertThat(result.getImpactPercent()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("shouldHandleLowercaseScenarioStringSameAsUppercase")
        void shouldHandleLowercaseScenarioStringSameAsUppercase() {
            // scenario.toUpperCase() in service means lowercase input works
            Asset stock = asset(assetId, "Energy", AssetType.STOCK, new BigDecimal("100.00"));
            Holding h = holding(assetId, new BigDecimal("100"), new BigDecimal("100.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(h));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(stock));

            StressTestResponse lower = stressTestService.runStressTest(userId, request("market_crash_20"));
            StressTestResponse upper = stressTestService.runStressTest(userId, request("MARKET_CRASH_20"));

            assertThat(lower.getImpactPercent())
                    .isEqualByComparingTo(upper.getImpactPercent());
        }

        @Test
        @DisplayName("shouldFallbackToAveragePriceWhenAssetNotFoundInRepository")
        void shouldFallbackToAveragePriceWhenAssetNotFoundInRepository() {
            // asset == null → applyScenarioShock returns price unchanged
            // price comes from holding.averagePrice as fallback in runStressTest
            Holding h = holding(assetId, new BigDecimal("100"), new BigDecimal("100.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(h));
            when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

            // asset is null → currentPrice = averagePrice = 100, shock returns price
            // unchanged
            StressTestResponse result = stressTestService.runStressTest(userId, request("MARKET_CRASH_20"));

            // preStress = postStress = 10000 (no shock applied when asset is null)
            assertThat(result.getPreStressValue()).isEqualByComparingTo("10000.00");
            assertThat(result.getPostStressValue()).isEqualByComparingTo("10000.00");
            assertThat(result.getImpactAmount()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("shouldReturnZeroImpactPercentWhenPreStressValueIsZero")
        void shouldReturnZeroImpactPercentWhenPreStressValueIsZero() {
            // quantity = 0 → preStressValue = 0 → no divide-by-zero
            Holding zeroHolding = Holding.builder()
                    .portfolioId(portfolioId)
                    .assetId(assetId)
                    .quantity(BigDecimal.ZERO)
                    .averagePrice(new BigDecimal("100.00"))
                    .build();

            Asset stock = asset(assetId, "Technology", AssetType.STOCK, new BigDecimal("100.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(zeroHolding));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(stock));

            StressTestResponse result = stressTestService.runStressTest(userId, request("MARKET_CRASH_20"));

            assertThat(result.getImpactPercent()).isEqualByComparingTo("0");
        }
    }
}