package com.sopuro.account_service.repositories;

import com.sopuro.account_service.entities.ProcessedPaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ProcessedPaymentRepository extends JpaRepository<ProcessedPaymentEntity, UUID> {
    Page<ProcessedPaymentEntity> findByIsSent(
            boolean isSent,
            Pageable pageable);

    @Modifying
    @Query("UPDATE ProcessedPaymentEntity p SET p.isSent = true WHERE p.paymentId IN :ids")
    int markAsSent(@Param("ids") List<UUID> ids);
}
