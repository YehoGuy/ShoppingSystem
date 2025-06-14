package com.example.app.DBLayer.AuthToken;

import java.lang.reflect.Member;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DomainLayer.IAuthTokenRepository;
import com.example.app.DomainLayer.AuthToken;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class AuthTokenRepositoryDBImpl implements IAuthTokenRepository {

    private final AuthTokenRepositoryDB jpaRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public AuthTokenRepositoryDBImpl(@Lazy @Autowired AuthTokenRepositoryDB jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public AuthToken getAuthToken(int userId) {
        return jpaRepo.findByMemberId(userId).orElse(null);
    }

    @Override
    public void setAuthToken(int userId, AuthToken token) {
        try {
            jpaRepo.save(token); // assumes token already has correct userId (memberId)
        } catch (Exception e) {
            throw new OurRuntime("Failed to set auth token for userId=" + userId, e);
        }
    }

    @Override
    public void removeAuthToken(int userId) {
        Optional<AuthToken> existing = jpaRepo.findByMemberId(userId);
        existing.ifPresent(jpaRepo::delete);
    }

    @Override
    public int getUserIdByToken(String token) {
        //TODO: implement
        return -1; // Token not found
    }
}
