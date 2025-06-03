package com.sopuro.account_service.entities;

import com.sopuro.account_service.enums.AccountStatus;
import com.sopuro.account_service.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountEntity {
  @EmbeddedId
  private AccountId id;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private AccountType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private AccountStatus status;

  @Column(name = "available_balance", precision = 19, scale = 2, nullable = false)
  @Builder.Default
  private BigDecimal availableBalance = BigDecimal.ZERO;

  @Column(name = "holding_balance", precision = 19, scale = 2, nullable = false)
  @Builder.Default
  private BigDecimal holdingBalance = BigDecimal.ZERO;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  @Builder.Default
  private Instant updatedAt = Instant.now();

  @PrePersist
  public void prePersist() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = Instant.now();
  }
}

