package com.example.app.DBLayer.AuthToken;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.AuthToken;

public interface AuthTokenRepositoryDB extends JpaRepository<AuthToken, String> {
    Optional<AuthToken> findByUserId(int userId);

    Optional<AuthToken> findByToken(String token);
}
