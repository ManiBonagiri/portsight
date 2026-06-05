package com.portsight.api.domains.market.controller;

import com.portsight.api.domains.market.service.MarketSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final MarketSimulationService marketSimulationService;

    public MarketController(MarketSimulationService marketSimulationService) {
        this.marketSimulationService = marketSimulationService;
    }

    /**
     * Triggers a market simulation for the current day.
     * Returns a simple success payload.
     */
    @PostMapping("/advance")
    public ResponseEntity<Map<String, Object>> advanceDay() {
        marketSimulationService.simulateMarketDay(LocalDate.now());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
