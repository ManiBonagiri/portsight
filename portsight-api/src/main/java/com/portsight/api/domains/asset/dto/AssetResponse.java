package com.portsight.api.domains.asset.dto;

import com.portsight.api.domains.asset.enums.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponse {
    private UUID id;
    private String ticker;
    private String name;
    private String sector;
    private AssetType assetType;
    private BigDecimal currentPrice;
}
