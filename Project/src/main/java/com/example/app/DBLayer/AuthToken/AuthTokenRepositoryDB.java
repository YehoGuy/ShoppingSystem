package com.example.app.DBLayer.AuthToken;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.DomainLayer.IAuthTokenRepository;
import com.example.app.DomainLayer.AuthToken;

@Repository
public interface AuthTokenRepositoryDB  extends JpaRepository<AuthToken, String> , IAuthTokenRepository{
    Optional<AuthToken> findByMemberId(int memberId);
    Optional<AuthToken> findByToken(String token);
}
