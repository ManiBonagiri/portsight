package com.portsight.api.domains.portfolio.mapper;

import com.portsight.api.domains.portfolio.dto.CreatePortfolioRequest;
import com.portsight.api.domains.portfolio.dto.PortfolioResponse;
import com.portsight.api.domains.portfolio.entity.Portfolio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PortfolioMapper {
    Portfolio toEntity(CreatePortfolioRequest request);
    PortfolioResponse toResponse(Portfolio portfolio);
}
