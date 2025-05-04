package InfrastructureLayer;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ApplicationLayer.OurArg;
import DomainLayer.AuthToken;
import DomainLayer.IAuthTokenRepository;

public class AuthTokenRepository implements IAuthTokenRepository {
    private Map<Integer, DomainLayer.AuthToken> authTokenMap; // Maps user IDs to their authentication tokens

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
