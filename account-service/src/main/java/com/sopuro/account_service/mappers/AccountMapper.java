package com.sopuro.account_service.mappers;

import com.sopuro.account_service.dtos.PrivateAccountDetailsDTO;
import com.sopuro.account_service.entities.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "number", expression = "java(accountEntity.getId().getNumber())")
    @Mapping(target = "ownerId", expression = "java(accountEntity.getId().getUserId())")
    PrivateAccountDetailsDTO toAccountDetailsDTO(AccountEntity accountEntity);
}
