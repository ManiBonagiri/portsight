package com.portsight.api.domains.asset.mapper;

import com.portsight.api.domains.asset.dto.AssetResponse;
import com.portsight.api.domains.asset.entity.Asset;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AssetMapper {
    AssetResponse toResponse(Asset asset);
}
