package com.sopuro.payment_service.mappers;

import com.sopuro.payment_service.dtos.PaymentDetailsDTO;
import com.sopuro.payment_service.entities.PaymentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentDetailsDTO toPaymentDetailsDTO(PaymentEntity paymentEntity);
}
