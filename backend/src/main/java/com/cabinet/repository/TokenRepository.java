package com.cabinet.repository;

import com.cabinet.entity.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<ApiToken, Long> {
    Optional<ApiToken> findByToken(String token);
    void deleteByToken(String token);
    boolean existsByToken(String token);
}

