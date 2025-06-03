package com.sopuro.account_service.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountId implements Serializable {

  @Column(name = "user_id", columnDefinition = "UUID", nullable = false)
  private UUID userId;

  @Column(name = "account_number", nullable = false)
  private String number;
}
