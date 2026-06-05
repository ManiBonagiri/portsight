package com.portsight.api.domains.holding.mapper;

import com.portsight.api.domains.holding.dto.HoldingResponse;
import com.portsight.api.domains.holding.entity.Holding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface HoldingMapper {
    @Mapping(target = "asset", ignore = true)
    @Mapping(target = "currentValue", ignore = true)
    @Mapping(target = "totalInvestment", ignore = true)
    @Mapping(target = "unrealizedGain", ignore = true)
    HoldingResponse toResponse(Holding holding);
}
