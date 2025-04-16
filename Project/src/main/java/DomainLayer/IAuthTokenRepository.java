package main.java.DomainLayer;

public interface IAuthTokenRepository {
    AuthToken getAuthToken(int userId); // Retrieves the authentication token for a given user ID

    void setAuthToken(int userId, String token); // Adds or updates the authentication token for a given user ID

    void removeAuthToken(int userId); // Removes the authentication token for a given user ID
    
}
