package com.sopuro.payment_service.repositories;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import com.sopuro.payment_service.entities.PaymentEntity;

public interface PaymentRepository extends CrudRepository<PaymentEntity, UUID> {
    Page<PaymentEntity> findBySenderIdOrRecipientId(UUID senderId, UUID recipentId, Pageable pageable);
}
