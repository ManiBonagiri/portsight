package com.portsight.api.domains.asset;

import com.portsight.api.domains.asset.dto.AssetResponse;
import com.portsight.api.domains.asset.enums.AssetType;
import com.portsight.api.domains.asset.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> searchAssets(
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) AssetType assetType) {
        
        List<AssetResponse> assets = assetService.searchAssets(ticker, sector, assetType);
        return ResponseEntity.ok(Map.of("success", true, "data", assets));
    }
}
