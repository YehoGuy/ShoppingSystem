package InfrastructureLayerTests;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DomainLayer.AuthToken;
import DomainLayer.IAuthTokenRepository;
import InfrastructureLayer.AuthTokenRepository;

public class AuthTokenRepoTests {
    private IAuthTokenRepository authTokenRepo;
    private AuthToken authToken1;
    private AuthToken authToken2;
    private Date expirationDate1; // Set expiration date to 10 seconds in the future
    private Date expirationDate2; // Set expiration date to 10 seconds in the future

    @BeforeEach
    public void setup() {
        authTokenRepo = new AuthTokenRepository();
        expirationDate1 = new Date(System.currentTimeMillis() + 1000000000000L); // Set expiration date to 10 seconds in the future
        expirationDate2 = new Date(System.currentTimeMillis() + 2000000000000L); // Set expiration date to 20 seconds in the future
        authToken1 = new AuthToken("token1", expirationDate1); // Set expiration date to 10 seconds in the future
        authToken2 = new AuthToken("token2", expirationDate2); // Set expiration date to 20 seconds in the future
    }

    @Test
    public void testSetAndGetAuthToken() {
        authTokenRepo.setAuthToken(1, authToken1);
        assertEquals(authToken1, authTokenRepo.getAuthToken(1));
    }

    @Test
    public void testRemoveAuthToken() {
        authTokenRepo.setAuthToken(1, authToken1);
        authTokenRepo.removeAuthToken(1);
        assertNull(authTokenRepo.getAuthToken(1));
    }

    @Test
    public void testGetUserIdByToken() {
        authTokenRepo.setAuthToken(1, authToken1);
        authTokenRepo.setAuthToken(2, authToken2);
        assertEquals(1, authTokenRepo.getUserIdByToken("token1"));
        assertEquals(2, authTokenRepo.getUserIdByToken("token2"));
        assertEquals(-1, authTokenRepo.getUserIdByToken("nonexistent_token"));
    }

    @Test
    public void testGetAuthToken() {
        authTokenRepo.setAuthToken(1, authToken1);
        assertEquals(authToken1, authTokenRepo.getAuthToken(1));
        assertNull(authTokenRepo.getAuthToken(2)); // No token set for user ID 2
    }

    @Test
    public void testGetUserIdByTokenNotFound() {
        authTokenRepo.setAuthToken(1, authToken1);
        assertEquals(-1, authTokenRepo.getUserIdByToken("nonexistent_token"));
    }

    @Test
    public void testGetUserIdByTokenMultipleTokens() {
        authTokenRepo.setAuthToken(1, authToken1);
        authTokenRepo.setAuthToken(2, authToken2);
        assertEquals(1, authTokenRepo.getUserIdByToken("token1"));
        assertEquals(2, authTokenRepo.getUserIdByToken("token2"));
        assertEquals(-1, authTokenRepo.getUserIdByToken("nonexistent_token"));
    }
}
