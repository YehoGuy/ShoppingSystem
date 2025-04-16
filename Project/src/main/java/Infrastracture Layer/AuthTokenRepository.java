import java.util.HashMap;
import DomainLayer.IAuthTokenRepository;
import DomainLayer.AuthToken;

public class AuthTokenRepository extends IAuthTokenRepository {
    private Map<Integer, DomainLayer.AuthToken> authTokenMap; // Maps user IDs to their authentication tokens

    public AuthTokenRepository() {
        authTokenMap = new HashMap<>();
    }

    /**
     * Retrieves the authentication token for a given user ID.
     * 
     * @param userId The unique identifier of the user.
     * @return The authentication token associated with the given user ID, or null if not found.
     */
    public DomainLayer.AuthToken getAuthToken(int userId) {
        return authTokenMap.get(userId);
    }

    /**
     * Adds or updates the authentication token for a given user ID.
     * 
     * @param userId The unique identifier of the user.
     * @param token The authentication token to be associated with the user ID.
     */
    public void setAuthToken(int userId, String token) {
        authTokenMap.put(userId, token);
    }

    /**
     * Removes the authentication token for a given user ID.
     * 
     * @param userId The unique identifier of the user whose token is to be removed.
     */
    public void removeAuthToken(int userId) {
        authTokenMap.remove(userId);
    }
    
}
