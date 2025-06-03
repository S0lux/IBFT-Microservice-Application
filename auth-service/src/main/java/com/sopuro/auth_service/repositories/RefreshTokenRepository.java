package com.sopuro.auth_service.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.sopuro.auth_service.entities.RefreshTokenEntity;
import com.sopuro.auth_service.entities.UserEntity;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByToken(String token);
    Optional<RefreshTokenEntity> findByTokenAndRevokedFalse(String token);
    
    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.user = ?1 AND r.revoked = false")
    void revokeAllUserTokens(UserEntity user);
}
