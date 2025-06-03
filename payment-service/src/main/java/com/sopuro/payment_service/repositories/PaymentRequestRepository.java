package com.sopuro.payment_service.repositories;

import com.sopuro.payment_service.entities.PaymentRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRequestRepository extends CrudRepository<PaymentRequestEntity, UUID> {
    Page<PaymentRequestEntity> findByIsSent(boolean isSent, PageRequest pageRequest);

    @Query("{ '_id': { $in: ?0 } }")
    @Update("{ $set: { 'isSent': true } }")
    int updateIsSentByIdIn(List<UUID> ids);
}
