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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HoldingService Tests")
class HoldingServiceTest {

    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private HoldingMapper holdingMapper;
    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetMapper assetMapper;

    @InjectMocks
    private HoldingService holdingService;

    // Shared test fixtures
    private UUID userId;
    private UUID portfolioId;
    private UUID assetId;
    private Portfolio portfolio;
    private Asset asset;
    private AssetResponse assetResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        assetId = UUID.randomUUID();

        portfolio = new Portfolio();
        portfolio.setUserId(userId);
        portfolio.setId(portfolioId);

        asset = new Asset();
        asset.setId(assetId);
        asset.setTicker("TECH01");
        asset.setCurrentPrice(new BigDecimal("150.00"));

        assetResponse = new AssetResponse();
        assetResponse.setTicker("TECH01");
        assetResponse.setCurrentPrice(new BigDecimal("150.00"));
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("addHolding — new holding")
    class AddHoldingNewTests {

        @Test
        @DisplayName("shouldCreateNewHoldingWhenNoneExists")
        void shouldCreateNewHoldingWhenNoneExists() {
            // Arrange
            AddHoldingRequest request = AddHoldingRequest.builder()
                    .assetId(assetId)
                    .quantity(new BigDecimal("100"))
                    .price(new BigDecimal("120.00"))
                    .build();

            Holding savedHolding = Holding.builder()
                    .id(UUID.randomUUID())
                    .portfolioId(portfolioId)
                    .assetId(assetId)
                    .quantity(new BigDecimal("100"))
                    .averagePrice(new BigDecimal("120.00"))
                    .build();

            HoldingResponse baseResponse = new HoldingResponse();
            baseResponse.setId(savedHolding.getId());
            baseResponse.setQuantity(new BigDecimal("100"));
            baseResponse.setAveragePrice(new BigDecimal("120.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(holdingRepository.findByPortfolioIdAndAssetId(portfolioId, assetId))
                    .thenReturn(Optional.empty());
            when(holdingRepository.save(any(Holding.class))).thenReturn(savedHolding);
            when(holdingMapper.toResponse(savedHolding)).thenReturn(baseResponse);
            when(assetMapper.toResponse(asset)).thenReturn(assetResponse);

            // Act
            HoldingResponse result = holdingService.addHolding(userId, portfolioId, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getQuantity()).isEqualByComparingTo("100");
            assertThat(result.getAveragePrice()).isEqualByComparingTo("120.00");
            assertThat(result.getAsset()).isNotNull();
            assertThat(result.getAsset().getTicker()).isEqualTo("TECH01");

            // currentValue = 100 × 150 = 15000
            assertThat(result.getCurrentValue()).isEqualByComparingTo("15000.00");
            // totalInvestment = 100 × 120 = 12000
            assertThat(result.getTotalInvestment()).isEqualByComparingTo("12000.00");
            // unrealizedGain = 15000 - 12000 = 3000
            assertThat(result.getUnrealizedGain()).isEqualByComparingTo("3000.00");
        }

        @Test
        @DisplayName("shouldSaveHoldingWithQuantityAndAveragePriceFromRequest")
        void shouldSaveHoldingWithQuantityAndAveragePriceFromRequest() {
            // Arrange
            AddHoldingRequest request = AddHoldingRequest.builder()
                    .assetId(assetId)
                    .quantity(new BigDecimal("50"))
                    .price(new BigDecimal("200.00"))
                    .build();

            Holding capturedHolding = Holding.builder()
                    .portfolioId(portfolioId)
                    .assetId(assetId)
                    .quantity(new BigDecimal("50"))
                    .averagePrice(new BigDecimal("200.00"))
                    .build();

            HoldingResponse baseResponse = new HoldingResponse();
            baseResponse.setQuantity(new BigDecimal("50"));
            baseResponse.setAveragePrice(new BigDecimal("200.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(holdingRepository.findByPortfolioIdAndAssetId(portfolioId, assetId))
                    .thenReturn(Optional.empty());
            when(holdingRepository.save(any(Holding.class))).thenReturn(capturedHolding);
            when(holdingMapper.toResponse(any())).thenReturn(baseResponse);
            when(assetMapper.toResponse(asset)).thenReturn(assetResponse);

            // Act
            holdingService.addHolding(userId, portfolioId, request);

            // Assert — capture what was actually saved
            ArgumentCaptor<Holding> captor = ArgumentCaptor.forClass(Holding.class);
            verify(holdingRepository).save(captor.capture());
            Holding saved = captor.getValue();

            assertThat(saved.getQuantity()).isEqualByComparingTo("50");
            // avgPrice for a brand-new holding = (0 + 50×200) / 50 = 200
            assertThat(saved.getAveragePrice()).isEqualByComparingTo("200.0000");
            assertThat(saved.getPortfolioId()).isEqualTo(portfolioId);
            assertThat(saved.getAssetId()).isEqualTo(assetId);
        }

        @Test
        @DisplayName("shouldFallbackToAveragePriceWhenAssetCurrentPriceIsNull")
        void shouldFallbackToAveragePriceWhenAssetCurrentPriceIsNull() {
            // currentPrice null → enrichHoldingResponse uses averagePrice as currentPrice
            asset.setCurrentPrice(null);

            AddHoldingRequest request = AddHoldingRequest.builder()
                    .assetId(assetId)
                    .quantity(new BigDecimal("100"))
                    .price(new BigDecimal("100.00"))
                    .build();

            Holding savedHolding = Holding.builder()
                    .portfolioId(portfolioId)
                    .assetId(assetId)
                    .quantity(new BigDecimal("100"))
                    .averagePrice(new BigDecimal("100.00"))
                    .build();

            HoldingResponse baseResponse = new HoldingResponse();
            baseResponse.setQuantity(new BigDecimal("100"));
            baseResponse.setAveragePrice(new BigDecimal("100.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(holdingRepository.findByPortfolioIdAndAssetId(portfolioId, assetId))
                    .thenReturn(Optional.empty());
            when(holdingRepository.save(any())).thenReturn(savedHolding);
            when(holdingMapper.toResponse(any())).thenReturn(baseResponse);
            when(assetMapper.toResponse(asset)).thenReturn(assetResponse);

            HoldingResponse result = holdingService.addHolding(userId, portfolioId, request);

            // currentValue = 100 × 100 (fallback to avgPrice) = 10000
            assertThat(result.getCurrentValue()).isEqualByComparingTo("10000.00");
            // unrealizedGain = 0 (price == avgPrice)
            assertThat(result.getUnrealizedGain()).isEqualByComparingTo("0.00");
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("addHolding — existing holding (weighted average)")
    class AddHoldingExistingTests {

        @Test
        @DisplayName("shouldUpdateWeightedAveragePriceWhenHoldingAlreadyExists")
        void shouldUpdateWeightedAveragePriceWhenHoldingAlreadyExists() {
            // Existing: 100 shares @ 120 → total = 12000
            // Adding: 100 shares @ 180 → total = 18000
            // New avg = (12000 + 18000) / 200 = 150.0000
            Holding existing = Holding.builder()
                    .id(UUID.randomUUID())
                    .portfolioId(portfolioId)
                    .assetId(assetId)
                    .quantity(new BigDecimal("100"))
                    .averagePrice(new BigDecimal("120.00"))
                    .build();

            AddHoldingRequest request = AddHoldingRequest.builder()
                    .assetId(assetId)
                    .quantity(new BigDecimal("100"))
                    .price(new BigDecimal("180.00"))
                    .build();

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(holdingRepository.findByPortfolioIdAndAssetId(portfolioId, assetId))
                    .thenReturn(Optional.of(existing));
            when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));

            HoldingResponse baseResponse = new HoldingResponse();
            baseResponse.setQuantity(new BigDecimal("200"));
            baseResponse.setAveragePrice(new BigDecimal("150.0000"));
            when(holdingMapper.toResponse(any())).thenReturn(baseResponse);
            when(assetMapper.toResponse(asset)).thenReturn(assetResponse);

            holdingService.addHolding(userId, portfolioId, request);

            ArgumentCaptor<Holding> captor = ArgumentCaptor.forClass(Holding.class);
            verify(holdingRepository).save(captor.capture());
            Holding saved = captor.getValue();

            assertThat(saved.getQuantity()).isEqualByComparingTo("200");
            assertThat(saved.getAveragePrice()).isEqualByComparingTo("150.0000");
        }

        @Test
        @DisplayName("shouldAccumulateQuantityOnTopOfExistingHolding")
        void shouldAccumulateQuantityOnTopOfExistingHolding() {
            Holding existing = Holding.builder()
                    .portfolioId(portfolioId)
                    .assetId(assetId)
                    .quantity(new BigDecimal("50"))
                    .averagePrice(new BigDecimal("100.00"))
                    .build();

            AddHoldingRequest request = AddHoldingRequest.builder()
                    .assetId(assetId)
                    .quantity(new BigDecimal("25"))
                    .price(new BigDecimal("100.00"))
                    .build();

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(holdingRepository.findByPortfolioIdAndAssetId(portfolioId, assetId))
                    .thenReturn(Optional.of(existing));
            when(holdingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            HoldingResponse baseResponse = new HoldingResponse();
            baseResponse.setQuantity(new BigDecimal("75"));
            when(holdingMapper.toResponse(any())).thenReturn(baseResponse);
            when(assetMapper.toResponse(asset)).thenReturn(assetResponse);

            holdingService.addHolding(userId, portfolioId, request);

            ArgumentCaptor<Holding> captor = ArgumentCaptor.forClass(Holding.class);
            verify(holdingRepository).save(captor.capture());
            assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("75");
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("addHolding — ownership & not-found guards")
    class AddHoldingOwnershipTests {

        @Test
        @DisplayName("shouldThrowWhenPortfolioNotFoundOnAdd")
        void shouldThrowWhenPortfolioNotFoundOnAdd() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            AddHoldingRequest request = AddHoldingRequest.builder()
                    .assetId(assetId)
                    .quantity(new BigDecimal("10"))
                    .price(new BigDecimal("100"))
                    .build();

            assertThatThrownBy(() -> holdingService.addHolding(userId, portfolioId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(holdingRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldThrowWhenUserDoesNotOwnPortfolioOnAdd")
        void shouldThrowWhenUserDoesNotOwnPortfolioOnAdd() {
            UUID differentUser = UUID.randomUUID();
            // portfolio belongs to userId, but we call with differentUser
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            AddHoldingRequest request = AddHoldingRequest.builder()
                    .assetId(assetId)
                    .quantity(new BigDecimal("10"))
                    .price(new BigDecimal("100"))
                    .build();

            assertThatThrownBy(() -> holdingService.addHolding(differentUser, portfolioId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(holdingRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldThrowWhenAssetNotFoundOnAdd")
        void shouldThrowWhenAssetNotFoundOnAdd() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

            AddHoldingRequest request = AddHoldingRequest.builder()
                    .assetId(assetId)
                    .quantity(new BigDecimal("10"))
                    .price(new BigDecimal("100"))
                    .build();

            assertThatThrownBy(() -> holdingService.addHolding(userId, portfolioId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Asset not found");

            verify(holdingRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getPortfolioHoldings")
    class GetPortfolioHoldingsTests {

        @Test
        @DisplayName("shouldReturnEnrichedHoldingsForPortfolio")
        void shouldReturnEnrichedHoldingsForPortfolio() {
            Holding h1 = Holding.builder()
                    .id(UUID.randomUUID())
                    .portfolioId(portfolioId)
                    .assetId(assetId)
                    .quantity(new BigDecimal("100"))
                    .averagePrice(new BigDecimal("120.00"))
                    .build();

            HoldingResponse r1 = new HoldingResponse();
            r1.setId(h1.getId());
            r1.setQuantity(new BigDecimal("100"));
            r1.setAveragePrice(new BigDecimal("120.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(h1));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(holdingMapper.toResponse(h1)).thenReturn(r1);
            when(assetMapper.toResponse(asset)).thenReturn(assetResponse);

            List<HoldingResponse> results = holdingService.getPortfolioHoldings(userId, portfolioId);

            assertThat(results).hasSize(1);
            HoldingResponse result = results.get(0);
            assertThat(result.getAsset()).isNotNull();
            // currentValue = 100 × 150 = 15000
            assertThat(result.getCurrentValue()).isEqualByComparingTo("15000.00");
            // totalInvestment = 100 × 120 = 12000
            assertThat(result.getTotalInvestment()).isEqualByComparingTo("12000.00");
            // unrealizedGain = 3000
            assertThat(result.getUnrealizedGain()).isEqualByComparingTo("3000.00");
        }

        @Test
        @DisplayName("shouldReturnEmptyListWhenPortfolioHasNoHoldings")
        void shouldReturnEmptyListWhenPortfolioHasNoHoldings() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of());

            List<HoldingResponse> results = holdingService.getPortfolioHoldings(userId, portfolioId);

            assertThat(results).isEmpty();
            verify(assetRepository, never()).findById(any());
        }

        @Test
        @DisplayName("shouldReturnMultipleHoldingsAllEnriched")
        void shouldReturnMultipleHoldingsAllEnriched() {
            UUID assetId2 = UUID.randomUUID();

            Asset asset2 = new Asset();
            asset2.setId(assetId2);
            asset2.setTicker("BANK01");
            asset2.setCurrentPrice(new BigDecimal("500.00"));

            AssetResponse assetResponse2 = new AssetResponse();
            assetResponse2.setTicker("BANK01");
            assetResponse2.setCurrentPrice(new BigDecimal("500.00"));

            Holding h1 = Holding.builder()
                    .id(UUID.randomUUID()).portfolioId(portfolioId).assetId(assetId)
                    .quantity(new BigDecimal("10")).averagePrice(new BigDecimal("100.00")).build();

            Holding h2 = Holding.builder()
                    .id(UUID.randomUUID()).portfolioId(portfolioId).assetId(assetId2)
                    .quantity(new BigDecimal("20")).averagePrice(new BigDecimal("400.00")).build();

            HoldingResponse r1 = new HoldingResponse();
            r1.setQuantity(new BigDecimal("10"));
            r1.setAveragePrice(new BigDecimal("100.00"));

            HoldingResponse r2 = new HoldingResponse();
            r2.setQuantity(new BigDecimal("20"));
            r2.setAveragePrice(new BigDecimal("400.00"));

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(holdingRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(h1, h2));
            when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
            when(assetRepository.findById(assetId2)).thenReturn(Optional.of(asset2));
            when(holdingMapper.toResponse(h1)).thenReturn(r1);
            when(holdingMapper.toResponse(h2)).thenReturn(r2);
            when(assetMapper.toResponse(asset)).thenReturn(assetResponse);
            when(assetMapper.toResponse(asset2)).thenReturn(assetResponse2);

            List<HoldingResponse> results = holdingService.getPortfolioHoldings(userId, portfolioId);

            assertThat(results).hasSize(2);
            // h1: currentValue = 10 × 150 = 1500, totalInvestment = 10 × 100 = 1000
            assertThat(results.get(0).getCurrentValue()).isEqualByComparingTo("1500.00");
            assertThat(results.get(0).getTotalInvestment()).isEqualByComparingTo("1000.00");
            // h2: currentValue = 20 × 500 = 10000, totalInvestment = 20 × 400 = 8000
            assertThat(results.get(1).getCurrentValue()).isEqualByComparingTo("10000.00");
            assertThat(results.get(1).getTotalInvestment()).isEqualByComparingTo("8000.00");
        }

        @Test
        @DisplayName("shouldThrowWhenPortfolioNotFoundOnGet")
        void shouldThrowWhenPortfolioNotFoundOnGet() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> holdingService.getPortfolioHoldings(userId, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(holdingRepository, never()).findByPortfolioId(any());
        }

        @Test
        @DisplayName("shouldThrowWhenUserDoesNotOwnPortfolioOnGet")
        void shouldThrowWhenUserDoesNotOwnPortfolioOnGet() {
            UUID differentUser = UUID.randomUUID();
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> holdingService.getPortfolioHoldings(differentUser, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(holdingRepository, never()).findByPortfolioId(any());
        }
    }
}