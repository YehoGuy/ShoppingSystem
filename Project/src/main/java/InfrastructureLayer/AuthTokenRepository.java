package InfrastructureLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import DomainLayer.IAuthTokenRepository;
import DomainLayer.AuthToken;

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
    
}
