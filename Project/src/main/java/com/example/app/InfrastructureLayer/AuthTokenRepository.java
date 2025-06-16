package com.example.app.InfrastructureLayer;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.app.ApplicationLayer.OurArg;
import com.example.app.DomainLayer.AuthToken;
import com.example.app.DomainLayer.IAuthTokenRepository;

@Repository
@Profile("no-db | test")
public class AuthTokenRepository implements IAuthTokenRepository {
    private Map<Integer, com.example.app.DomainLayer.AuthToken> authTokenMap; // Maps user IDs to their authentication tokens

    public AuthTokenRepository() {
        authTokenMap = new ConcurrentHashMap<>();
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

    public void setAuthToken(int userId, AuthToken token) {
        if (token == null || token.getToken() == null) {
            throw new OurArg("Token cannot be null");
        }
        if (userId <= 0) {
            throw new OurArg("User ID must be positive");
        }
        if(token.getExpirationTime().after(new Date()))
            authTokenMap.put(userId, token);
        else
            throw new OurArg("Token has expired");
    }

    public void removeAuthToken(int userId) {
        authTokenMap.remove(userId);
    }

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
