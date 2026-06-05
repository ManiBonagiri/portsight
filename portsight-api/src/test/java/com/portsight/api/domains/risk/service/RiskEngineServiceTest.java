package com.portsight.api.domains.risk.service;

import com.portsight.api.domains.asset.entity.Asset;
import com.portsight.api.domains.asset.repository.AssetRepository;
import com.portsight.api.domains.holding.entity.Holding;
import com.portsight.api.domains.holding.repository.HoldingRepository;
import com.portsight.api.domains.market.entity.AssetPriceHistory;
import com.portsight.api.domains.market.repository.AssetPriceHistoryRepository;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.enums.RiskProfile;
import com.portsight.api.domains.portfolio.repository.PortfolioRepository;
import com.portsight.api.domains.risk.dto.RiskResponse;
import com.portsight.api.domains.risk.entity.RiskMetrics;
import com.portsight.api.domains.risk.repository.RiskMetricsRepository;
import com.portsight.api.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskEngineService Unit Tests")
class RiskEngineServiceTest {

    @Mock
    private RiskMetricsRepository riskMetricsRepository;
    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetPriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private RiskEngineService riskEngineService;

    private UUID userId;
    private UUID portfolioId;
    private UUID assetId;
    private Portfolio portfolio;
    private RiskMetrics riskMetrics;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        assetId = UUID.randomUUID();

        portfolio = Portfolio.builder()
                .id(portfolioId)
                .userId(userId)
                .portfolioName("Test Portfolio")
                .riskProfile(RiskProfile.MODERATE)
                .benchmark("SIM_NIFTY")
                .status("ACTIVE")
                .build();

        riskMetrics = RiskMetrics.builder()
                .id(UUID.randomUUID())
                .portfolioId(portfolioId)
                .volatility(new BigDecimal("12.5000"))
                .beta(new BigDecimal("1.0000"))
                .sharpeRatio(new BigDecimal("1.4500"))
                .var95(new BigDecimal("-8.2000"))
                .build();
    }

    // -----------------------------------------------------------------------
    // GET RISK METRICS
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getRiskMetrics")
    class GetRiskMetricsTests {

        @Test
        @DisplayName("shouldReturnRiskMetricsForPortfolioOwner")
        void shouldReturnRiskMetricsForPortfolioOwner() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(riskMetricsRepository.findByPortfolioId(portfolioId)).thenReturn(Optional.of(riskMetrics));

            RiskResponse result = riskEngineService.getRiskMetrics(userId, portfolioId);

            assertThat(result).isNotNull();
            assertThat(result.getVolatility()).isEqualByComparingTo(new BigDecimal("12.5000"));
            assertThat(result.getBeta()).isEqualByComparingTo(new BigDecimal("1.0000"));
            assertThat(result.getSharpeRatio()).isEqualByComparingTo(new BigDecimal("1.4500"));
            assertThat(result.getVar95()).isEqualByComparingTo(new BigDecimal("-8.2000"));
        }

        @Test
        @DisplayName("shouldThrowWhenPortfolioNotFound")
        void shouldThrowWhenPortfolioNotFound() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> riskEngineService.getRiskMetrics(userId, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("shouldThrowWhenUserDoesNotOwnPortfolio")
        void shouldThrowWhenUserDoesNotOwnPortfolio() {
            UUID differentUserId = UUID.randomUUID();
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> riskEngineService.getRiskMetrics(differentUserId, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("shouldThrowWhenRiskMetricsNotYetCalculated")
        void shouldThrowWhenRiskMetricsNotYetCalculated() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(riskMetricsRepository.findByPortfolioId(portfolioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> riskEngineService.getRiskMetrics(userId, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(portfolioId.toString());
        }
    }

    // -----------------------------------------------------------------------
    // CALCULATE PORTFOLIO RISK
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("calculatePortfolioRisk")
    class CalculatePortfolioRiskTests {

        @Test
        @DisplayName("shouldSkipCalculationWhenNoHoldings")
        void shouldSkipCalculationWhenNoHoldings() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(Collections.emptyList());

            riskEngineService.calculatePortfolioRisk(portfolioId);

            verify(riskMetricsRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldThrowWhenPortfolioNotFoundForCalculation")
        void shouldThrowWhenPortfolioNotFoundForCalculation() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> riskEngineService.calculatePortfolioRisk(portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(riskMetricsRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldUseFallbackWhenInsufficientPriceHistory")
        void shouldUseFallbackWhenInsufficientPriceHistory() {
            Holding holding = buildHolding(assetId, new BigDecimal("100"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            // Return empty price history — triggers fallback
            when(priceHistoryRepository.findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                    eq(assetId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(riskMetricsRepository.findByPortfolioId(portfolioId)).thenReturn(Optional.empty());

            riskEngineService.calculatePortfolioRisk(portfolioId);

            // Fallback must save metrics with MODERATE profile defaults
            ArgumentCaptor<RiskMetrics> captor = ArgumentCaptor.forClass(RiskMetrics.class);
            verify(riskMetricsRepository).save(captor.capture());
            RiskMetrics saved = captor.getValue();
            assertThat(saved.getVolatility()).isEqualByComparingTo(new BigDecimal("12.5"));
            assertThat(saved.getBeta()).isEqualByComparingTo(new BigDecimal("1.0"));
        }

        @Test
        @DisplayName("shouldUseFallbackValuesForConservativeProfile")
        void shouldUseFallbackValuesForConservativeProfile() {
            Portfolio conservativePortfolio = Portfolio.builder()
                    .id(portfolioId)
                    .userId(userId)
                    .portfolioName("Conservative")
                    .riskProfile(RiskProfile.CONSERVATIVE)
                    .benchmark("SIM_NIFTY")
                    .status("ACTIVE")
                    .build();

            Holding holding = buildHolding(assetId, new BigDecimal("100"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(conservativePortfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(priceHistoryRepository.findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                    eq(assetId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(riskMetricsRepository.findByPortfolioId(portfolioId)).thenReturn(Optional.empty());

            riskEngineService.calculatePortfolioRisk(portfolioId);

            ArgumentCaptor<RiskMetrics> captor = ArgumentCaptor.forClass(RiskMetrics.class);
            verify(riskMetricsRepository).save(captor.capture());
            RiskMetrics saved = captor.getValue();
            assertThat(saved.getVolatility()).isEqualByComparingTo(new BigDecimal("5.2"));
            assertThat(saved.getBeta()).isEqualByComparingTo(new BigDecimal("0.6"));
        }

        @Test
        @DisplayName("shouldUseFallbackValuesForAggressiveProfile")
        void shouldUseFallbackValuesForAggressiveProfile() {
            Portfolio aggressivePortfolio = Portfolio.builder()
                    .id(portfolioId)
                    .userId(userId)
                    .portfolioName("Aggressive")
                    .riskProfile(RiskProfile.AGGRESSIVE)
                    .benchmark("SIM_NIFTY")
                    .status("ACTIVE")
                    .build();

            Holding holding = buildHolding(assetId, new BigDecimal("100"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(aggressivePortfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(priceHistoryRepository.findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                    eq(assetId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(riskMetricsRepository.findByPortfolioId(portfolioId)).thenReturn(Optional.empty());

            riskEngineService.calculatePortfolioRisk(portfolioId);

            ArgumentCaptor<RiskMetrics> captor = ArgumentCaptor.forClass(RiskMetrics.class);
            verify(riskMetricsRepository).save(captor.capture());
            RiskMetrics saved = captor.getValue();
            assertThat(saved.getVolatility()).isEqualByComparingTo(new BigDecimal("18.4"));
            assertThat(saved.getBeta()).isEqualByComparingTo(new BigDecimal("1.5"));
        }

        @Test
        @DisplayName("shouldCalculateAndSaveRealRiskMetricsWhenPriceHistoryExists")
        void shouldCalculateAndSaveRealRiskMetricsWhenPriceHistoryExists() {
            Holding holding = buildHolding(assetId, new BigDecimal("100"));

            // Build 30 days of realistic price history
            List<AssetPriceHistory> priceHistory = buildPriceHistory(assetId, 30, 1500.0, 0.01);

            Asset benchmarkAsset = new Asset();
            benchmarkAsset.setId(UUID.randomUUID());

            List<AssetPriceHistory> benchmarkHistory = buildPriceHistory(benchmarkAsset.getId(), 30, 22000.0, 0.008);

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(priceHistoryRepository.findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                    eq(assetId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(priceHistory);
            when(assetRepository.findByTicker("SIM_NIFTY")).thenReturn(Optional.of(benchmarkAsset));
            when(priceHistoryRepository.findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                    eq(benchmarkAsset.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(benchmarkHistory);
            when(riskMetricsRepository.findByPortfolioId(portfolioId)).thenReturn(Optional.empty());

            riskEngineService.calculatePortfolioRisk(portfolioId);

            ArgumentCaptor<RiskMetrics> captor = ArgumentCaptor.forClass(RiskMetrics.class);
            verify(riskMetricsRepository).save(captor.capture());
            RiskMetrics saved = captor.getValue();

            // Real calculations should produce non-zero values
            assertThat(saved.getVolatility()).isNotNull();
            assertThat(saved.getBeta()).isNotNull();
            assertThat(saved.getSharpeRatio()).isNotNull();
            assertThat(saved.getVar95()).isNotNull();
            assertThat(saved.getPortfolioId()).isEqualTo(portfolioId);
        }

        @Test
        @DisplayName("shouldUpdateExistingRiskMetricsRatherThanCreateNew")
        void shouldUpdateExistingRiskMetricsRatherThanCreateNew() {
            Holding holding = buildHolding(assetId, new BigDecimal("100"));
            List<AssetPriceHistory> priceHistory = buildPriceHistory(assetId, 30, 1500.0, 0.01);

            Asset benchmarkAsset = new Asset();
            benchmarkAsset.setId(UUID.randomUUID());
            List<AssetPriceHistory> benchmarkHistory = buildPriceHistory(benchmarkAsset.getId(), 30, 22000.0, 0.008);

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(holding));
            when(priceHistoryRepository.findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                    eq(assetId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(priceHistory);
            when(assetRepository.findByTicker("SIM_NIFTY")).thenReturn(Optional.of(benchmarkAsset));
            when(priceHistoryRepository.findByAssetIdAndRecordDateBetweenOrderByRecordDateAsc(
                    eq(benchmarkAsset.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(benchmarkHistory);
            // Return existing metrics — should update, not create new
            when(riskMetricsRepository.findByPortfolioId(portfolioId)).thenReturn(Optional.of(riskMetrics));

            riskEngineService.calculatePortfolioRisk(portfolioId);

            // save called once with the existing entity
            verify(riskMetricsRepository, times(1)).save(riskMetrics);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Holding buildHolding(UUID assetId, BigDecimal quantity) {
        Holding h = new Holding();
        h.setId(UUID.randomUUID());
        h.setPortfolioId(portfolioId);
        h.setAssetId(assetId);
        h.setQuantity(quantity);
        h.setAveragePrice(new BigDecimal("1500.00"));
        return h;
    }

    private List<AssetPriceHistory> buildPriceHistory(UUID assetId, int days,
            double startPrice, double dailyVolatility) {
        List<AssetPriceHistory> history = new java.util.ArrayList<>();
        double price = startPrice;
        LocalDate date = LocalDate.now().minusDays(days);
        java.util.Random random = new java.util.Random(42); // fixed seed for determinism

        for (int i = 0; i < days; i++) {
            double change = 1.0 + (random.nextDouble() - 0.5) * 2 * dailyVolatility;
            price = price * change;

            AssetPriceHistory aph = new AssetPriceHistory();
            aph.setAssetId(assetId);
            aph.setRecordDate(date.plusDays(i));
            aph.setClosingPrice(BigDecimal.valueOf(price));
            history.add(aph);
        }
        return history;
    }
}