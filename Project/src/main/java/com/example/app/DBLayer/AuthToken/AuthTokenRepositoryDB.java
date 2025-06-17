package com.example.app.DBLayer.AuthToken;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.AuthToken;
import com.example.app.DomainLayer.IAuthTokenRepository;

@Profile("!no-db & !test")
public interface AuthTokenRepositoryDB  extends JpaRepository<AuthToken, String> , IAuthTokenRepository{
    Optional<AuthToken> findByUserId(int userId);
    Optional<AuthToken> findByToken(String token);
}
