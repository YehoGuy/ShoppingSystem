import java.util.HashMap;
import DomainLayer.IAuthTokenRepository;
import DomainLayer.AuthToken;

public class AuthTokenRepository extends IAuthTokenRepository {
    private Map<Integer, DomainLayer.AuthToken> authTokenMap; // Maps user IDs to their authentication tokens

    public AuthTokenRepository() {
        authTokenMap = new HashMap<>();
    }

    public DomainLayer.AuthToken getAuthToken(int userId) {
        return authTokenMap.get(userId);
    }

    public void setAuthToken(int userId, DomainLayer.AuthToken token) {
        authTokenMap.put(userId, token);
    }

    public void removeAuthToken(int userId) {
        authTokenMap.remove(userId);
    }
    
}
