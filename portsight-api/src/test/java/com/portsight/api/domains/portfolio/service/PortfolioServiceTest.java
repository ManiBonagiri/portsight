package com.portsight.api.domains.portfolio.service;

import com.portsight.api.domains.portfolio.dto.CreatePortfolioRequest;
import com.portsight.api.domains.portfolio.dto.PortfolioResponse;
import com.portsight.api.domains.portfolio.dto.UpdatePortfolioRequest;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import com.portsight.api.domains.portfolio.enums.RiskProfile;
import com.portsight.api.domains.portfolio.mapper.PortfolioMapper;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService Unit Tests")
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioMapper portfolioMapper;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private UUID userId;
    private UUID portfolioId;
    private Portfolio portfolio;
    private PortfolioResponse portfolioResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();

        portfolio = Portfolio.builder()
                .id(portfolioId)
                .userId(userId)
                .portfolioName("Test Portfolio")
                .riskProfile(RiskProfile.MODERATE)
                .benchmark("SIM_NIFTY")
                .status("ACTIVE")
                .build();

        portfolioResponse = PortfolioResponse.builder()
                .id(portfolioId)
                .userId(userId)
                .portfolioName("Test Portfolio")
                .riskProfile(RiskProfile.MODERATE)
                .benchmark("SIM_NIFTY")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createPortfolio")
    class CreatePortfolioTests {

        @Test
        @DisplayName("shouldCreatePortfolioSuccessfully")
        void shouldCreatePortfolioSuccessfully() {
            CreatePortfolioRequest request = new CreatePortfolioRequest();
            request.setPortfolioName("Test Portfolio");
            request.setRiskProfile(RiskProfile.MODERATE);
            request.setBenchmark("SIM_NIFTY");

            when(portfolioMapper.toEntity(request)).thenReturn(portfolio);
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(portfolioMapper.toResponse(portfolio)).thenReturn(portfolioResponse);

            PortfolioResponse result = portfolioService.createPortfolio(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getPortfolioName()).isEqualTo("Test Portfolio");
            assertThat(result.getRiskProfile()).isEqualTo(RiskProfile.MODERATE);
            verify(portfolioRepository).save(portfolio);
        }

        @Test
        @DisplayName("shouldSetUserIdAndActiveStatusOnCreate")
        void shouldSetUserIdAndActiveStatusOnCreate() {
            CreatePortfolioRequest request = new CreatePortfolioRequest();
            request.setPortfolioName("New Portfolio");
            request.setRiskProfile(RiskProfile.AGGRESSIVE);
            request.setBenchmark("SIM_NIFTY");

            Portfolio blankPortfolio = new Portfolio();
            when(portfolioMapper.toEntity(request)).thenReturn(blankPortfolio);
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(blankPortfolio);
            when(portfolioMapper.toResponse(blankPortfolio)).thenReturn(portfolioResponse);

            portfolioService.createPortfolio(userId, request);

            assertThat(blankPortfolio.getUserId()).isEqualTo(userId);
            assertThat(blankPortfolio.getStatus()).isEqualTo("ACTIVE");
        }
    }

    // -----------------------------------------------------------------------
    // READ
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getUserPortfolios")
    class GetUserPortfoliosTests {

        @Test
        @DisplayName("shouldReturnAllActivePortfoliosForUser")
        void shouldReturnAllActivePortfoliosForUser() {
            when(portfolioRepository.findByUserIdAndStatus(userId, "ACTIVE"))
                    .thenReturn(List.of(portfolio));
            when(portfolioMapper.toResponse(portfolio)).thenReturn(portfolioResponse);

            List<PortfolioResponse> result = portfolioService.getUserPortfolios(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPortfolioName()).isEqualTo("Test Portfolio");
        }

        @Test
        @DisplayName("shouldReturnEmptyListWhenUserHasNoPortfolios")
        void shouldReturnEmptyListWhenUserHasNoPortfolios() {
            when(portfolioRepository.findByUserIdAndStatus(userId, "ACTIVE"))
                    .thenReturn(List.of());

            List<PortfolioResponse> result = portfolioService.getUserPortfolios(userId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPortfolio")
    class GetPortfolioTests {

        @Test
        @DisplayName("shouldReturnPortfolioWhenOwnerRequests")
        void shouldReturnPortfolioWhenOwnerRequests() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(portfolioMapper.toResponse(portfolio)).thenReturn(portfolioResponse);

            PortfolioResponse result = portfolioService.getPortfolio(userId, portfolioId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(portfolioId);
        }

        @Test
        @DisplayName("shouldThrowWhenPortfolioNotFound")
        void shouldThrowWhenPortfolioNotFound() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> portfolioService.getPortfolio(userId, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(portfolioId.toString());
        }

        @Test
        @DisplayName("shouldThrowWhenUserDoesNotOwnPortfolio")
        void shouldThrowWhenUserDoesNotOwnPortfolio() {
            UUID differentUserId = UUID.randomUUID();
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> portfolioService.getPortfolio(differentUserId, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updatePortfolio")
    class UpdatePortfolioTests {

        @Test
        @DisplayName("shouldUpdatePortfolioNameSuccessfully")
        void shouldUpdatePortfolioNameSuccessfully() {
            UpdatePortfolioRequest request = new UpdatePortfolioRequest();
            request.setPortfolioName("Updated Name");

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(portfolioMapper.toResponse(portfolio)).thenReturn(portfolioResponse);

            portfolioService.updatePortfolio(userId, portfolioId, request);

            assertThat(portfolio.getPortfolioName()).isEqualTo("Updated Name");
            verify(portfolioRepository).save(portfolio);
        }

        @Test
        @DisplayName("shouldUpdateOnlyNonNullFields")
        void shouldUpdateOnlyNonNullFields() {
            UpdatePortfolioRequest request = new UpdatePortfolioRequest();
            request.setPortfolioName("New Name");
            // riskProfile and benchmark left null intentionally

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);
            when(portfolioMapper.toResponse(portfolio)).thenReturn(portfolioResponse);

            portfolioService.updatePortfolio(userId, portfolioId, request);

            assertThat(portfolio.getPortfolioName()).isEqualTo("New Name");
            assertThat(portfolio.getRiskProfile()).isEqualTo(RiskProfile.MODERATE); // unchanged
            assertThat(portfolio.getBenchmark()).isEqualTo("SIM_NIFTY"); // unchanged
        }

        @Test
        @DisplayName("shouldThrowWhenUpdatingPortfolioNotOwned")
        void shouldThrowWhenUpdatingPortfolioNotOwned() {
            UUID differentUserId = UUID.randomUUID();
            UpdatePortfolioRequest request = new UpdatePortfolioRequest();
            request.setPortfolioName("Hacked Name");

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> portfolioService.updatePortfolio(differentUserId, portfolioId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(portfolioRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // ARCHIVE
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("archivePortfolio")
    class ArchivePortfolioTests {

        @Test
        @DisplayName("shouldArchivePortfolioSuccessfully")
        void shouldArchivePortfolioSuccessfully() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);

            portfolioService.archivePortfolio(userId, portfolioId);

            assertThat(portfolio.getStatus()).isEqualTo("ARCHIVED");
            verify(portfolioRepository).save(portfolio);
        }

        @Test
        @DisplayName("shouldThrowWhenArchivingPortfolioNotFound")
        void shouldThrowWhenArchivingPortfolioNotFound() {
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> portfolioService.archivePortfolio(userId, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldThrowWhenArchivingPortfolioNotOwned")
        void shouldThrowWhenArchivingPortfolioNotOwned() {
            UUID differentUserId = UUID.randomUUID();
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> portfolioService.archivePortfolio(differentUserId, portfolioId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(portfolioRepository, never()).save(any());
        }
    }
}