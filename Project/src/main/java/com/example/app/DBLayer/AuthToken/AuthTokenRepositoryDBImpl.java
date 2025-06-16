package com.example.app.DBLayer.AuthToken;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DomainLayer.AuthToken;
import com.example.app.DomainLayer.IAuthTokenRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
@Profile("!no-db & !test")
public class AuthTokenRepositoryDBImpl implements IAuthTokenRepository {

    private final AuthTokenRepositoryDB jpaRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public AuthTokenRepositoryDBImpl(@Lazy @Autowired AuthTokenRepositoryDB jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public AuthToken getAuthToken(int userId) {
        return jpaRepo.findByUserId(userId).orElse(null);
    }

    @Override
    public void setAuthToken(int userId, AuthToken token) {
        try {
            token.setUserId(userId);
            jpaRepo.save(token); // assumes token already has correct userId (memberId)
        } catch (Exception e) {
            throw new OurRuntime("Failed to set auth token for userId=" + userId, e);
        }
    }

    @Override
    public void removeAuthToken(int userId) {
        Optional<AuthToken> existing = jpaRepo.findByUserId(userId);
        existing.ifPresent(jpaRepo::delete);
    }

    @Override
    public int getUserIdByToken(String token) {
        return jpaRepo.findByToken(token).map(AuthToken::getUserId).orElse(-1);
    }
}
