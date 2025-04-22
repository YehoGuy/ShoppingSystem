package InfrastructureLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import DomainLayer.AuthToken;
import DomainLayer.IAuthTokenRepository;

public class AuthTokenRepository implements IAuthTokenRepository {
    private Map<Integer, DomainLayer.AuthToken> authTokenMap; // Maps user IDs to their authentication tokens

    public AuthTokenRepository() {
        authTokenMap = new ConcurrentHashMap<>();
    }

    public AuthToken getAuthToken(int userId) {
        return authTokenMap.get(userId);
    }

    public void setAuthToken(int userId, AuthToken token) {
        authTokenMap.put(userId, token);
    }

    public void removeAuthToken(int userId) {
        authTokenMap.remove(userId);
    }

    public int getUserIdByToken(String token) {
        for (Map.Entry<Integer, AuthToken> entry : authTokenMap.entrySet()) {
            if (entry.getValue().getToken().equals(token)) {
                return entry.getKey();
            }
        }
        return -1; // Token not found
    }
    
}
