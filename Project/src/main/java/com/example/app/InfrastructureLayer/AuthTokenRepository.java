package com.example.app.InfrastructureLayer;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.example.app.ApplicationLayer.OurArg;
import com.example.app.DataLayer.AuthTokenJpaRepository;
import com.example.app.DomainLayer.AuthToken;
import com.example.app.DomainLayer.IAuthTokenRepository;

import jakarta.transaction.Transactional;

@Repository
public class AuthTokenRepository implements IAuthTokenRepository {

    private final Map<Integer, com.example.app.DomainLayer.AuthToken> authTokenMap; // Maps user IDs to their authentication tokens

    private final AuthTokenJpaRepository authTokenJpaRepository;
    private final boolean isTestMode;

    @Autowired
    public AuthTokenRepository(AuthTokenJpaRepository authTokenJpaRepository) {
        this.authTokenMap = new ConcurrentHashMap<>();
        this.authTokenJpaRepository = authTokenJpaRepository;
        this.isTestMode = false; 
    }

    // test constructor
    public AuthTokenRepository() {
        authTokenMap = new ConcurrentHashMap<>();
        isTestMode = true;
        this.authTokenJpaRepository = null; 
    }

    public AuthToken getAuthToken(int userId) {
        AuthToken token = authTokenMap.get(userId);
        if(token == null) {
            return null; // Token not found
        }
        if(token.isExpired()){
            removeAuthToken(userId);
            return null;
        }
        return token;
    }

    @Override
    @Transactional
    public void setAuthToken(int userId, AuthToken token) {
        if (token == null || token.getToken() == null) {
            throw new OurArg("Token cannot be null");
        }
        if (userId <= 0) {
            throw new OurArg("User ID must be positive");
        }
        if(token.getExpirationTime().after(new Date())){
            authTokenMap.put(userId, token);
            if(!isTestMode) 
                authTokenJpaRepository.save(token); // Save to database if not in test mode
        }            
        else
            throw new OurArg("Token has expired");
    }

    @Override
    public void removeAuthToken(int userId) {
        authTokenMap.remove(userId);
    }

    @Override
    public int getUserIdByToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new OurArg("Token cannot be null");
        }
        for (Map.Entry<Integer, AuthToken> entry : authTokenMap.entrySet()) {
            if (entry.getValue().getToken().equals(token)) {
                return entry.getKey();
            }
        }
        return -1; // Token not found
    }
    
}
