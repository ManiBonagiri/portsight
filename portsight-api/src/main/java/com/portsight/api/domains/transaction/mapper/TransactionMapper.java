package com.portsight.api.domains.transaction.mapper;

import com.portsight.api.domains.transaction.dto.TransactionRequest;
import com.portsight.api.domains.transaction.dto.TransactionResponse;
import com.portsight.api.domains.transaction.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(TransactionRequest request);

    @Mapping(target = "asset", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    TransactionResponse toResponse(Transaction transaction);
}
