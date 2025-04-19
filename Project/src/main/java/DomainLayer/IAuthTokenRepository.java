package DomainLayer;

public interface IAuthTokenRepository {
    
    /**
     * Retrieves the authentication token for a given user ID.
     * 
     * @param userId The unique identifier of the user.
     * @return The authentication token associated with the given user ID, or null if not found.
     */
    AuthToken getAuthToken(int userId);

    /**
     * Adds or updates the authentication token for a given user ID.
     * 
     * @param userId The unique identifier of the user.
     * @param token The authentication token to be associated with the user ID.
     */
    void setAuthToken(int userId, AuthToken token);

    /**
     * Removes the authentication token for a given user ID.
     * 
     * @param userId The unique identifier of the user whose token is to be removed.
     */
    void removeAuthToken(int userId);

    /**
     * Retrieves the user ID associated with a given authentication token.
     * 
     * @param token The authentication token for which to find the associated user ID.
     * @return The user ID associated with the given token, or -1 if not found.
     */
    int getUserIdByToken(String token);
    
}
