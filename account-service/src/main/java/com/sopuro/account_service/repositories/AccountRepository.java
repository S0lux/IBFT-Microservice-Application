package com.sopuro.account_service.repositories;

import com.sopuro.account_service.entities.AccountEntity;
import com.sopuro.account_service.entities.AccountId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, AccountId> {
    Page<AccountEntity> findById_UserId(UUID userId, Pageable pageable);
    Optional<AccountEntity> findById_Number(String number);

    @Modifying
    @Query("UPDATE AccountEntity a SET a.holdingBalance = a.holdingBalance - :amount " +
            "WHERE a.id.number = :accountNumber AND a.holdingBalance >= :amount")
    int subtractHoldingBalance(@Param("accountNumber") String accountNumber,
                               @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE AccountEntity a SET a.holdingBalance = a.holdingBalance + :amount " +
            "WHERE a.id.number = :accountNumber")
    int addHoldingBalance(@Param("accountNumber") String accountNumber,
                          @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE AccountEntity a SET a.availableBalance = a.availableBalance - :amount " +
            "WHERE a.id.number = :accountNumber AND a.availableBalance >= :amount")
    int subtractAvailableBalance(@Param("accountNumber") String accountNumber,
                                 @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE AccountEntity a SET a.availableBalance = a.availableBalance + :amount " +
            "WHERE a.id.number = :accountNumber")
    int addAvailableBalance(@Param("accountNumber") String accountNumber,
                            @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE AccountEntity a SET " +
            "a.availableBalance = a.availableBalance - :amount, " +
            "a.holdingBalance = a.holdingBalance + :amount " +
            "WHERE a.id.number = :accountNumber AND a.availableBalance >= :amount")
    int withholdAvailableBalance(@Param("accountNumber") String accountNumber,
                                 @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE AccountEntity a SET " +
            "a.holdingBalance = a.holdingBalance - :amount, " +
            "a.availableBalance = a.availableBalance + :amount " +
            "WHERE a.id.number = :accountNumber AND a.holdingBalance >= :amount")
    int releaseWithheldBalance(@Param("accountNumber") String accountNumber,
                                 @Param("amount") BigDecimal amount);

    Boolean existsById_Number(String accountNumber);
}
