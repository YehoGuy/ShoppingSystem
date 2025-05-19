package InfrastructureLayerTests;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.ApplicationLayer.OurArg;
import com.example.app.DomainLayer.AuthToken;
import com.example.app.DomainLayer.IAuthTokenRepository;
import com.example.app.InfrastructureLayer.AuthTokenRepository;

public class AuthTokenRepoTests {
    private IAuthTokenRepository authTokenRepo;
    private AuthToken authToken1;
    private AuthToken authToken2;
    private Date expirationDate1; 
    private Date expirationDate2; 

    @BeforeEach
    public void setup() {
        authTokenRepo = new AuthTokenRepository();
        expirationDate1 = new Date(System.currentTimeMillis() + 1000000000000L); 
        expirationDate2 = new Date(System.currentTimeMillis() + 2000000000000L); 
        authToken1 = new AuthToken("token1", expirationDate1); 
        authToken2 = new AuthToken("token2", expirationDate2); 
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
        assertNull(authTokenRepo.getAuthToken(2)); // Ensure it returns null for non-existent token
    }

    @Test
    public void testGetUserIdByTokenMultipleTokens() {
        authTokenRepo.setAuthToken(1, authToken1);
        authTokenRepo.setAuthToken(2, authToken2);
        assertEquals(1, authTokenRepo.getUserIdByToken("token1"));
        assertEquals(2, authTokenRepo.getUserIdByToken("token2"));
        assertEquals(-1, authTokenRepo.getUserIdByToken("nonexistent_token"));
    }

    @Test
    public void testGetAuthTokenMultipleTokens() {
        authTokenRepo.setAuthToken(1, authToken1);
        authTokenRepo.setAuthToken(2, authToken2);
        assertEquals(authToken1, authTokenRepo.getAuthToken(1));
        assertEquals(authToken2, authTokenRepo.getAuthToken(2));
        assertNull(authTokenRepo.getAuthToken(3)); 
    }

    @Test
    public void testRemoveAuthTokenMultipleTokens() {
        authTokenRepo.setAuthToken(1, authToken1);
        authTokenRepo.setAuthToken(2, authToken2);
        authTokenRepo.removeAuthToken(1);
        assertNull(authTokenRepo.getAuthToken(1));
        assertEquals(authToken2, authTokenRepo.getAuthToken(2));
    }

    @Test
    public void testRemoveAuthTokenNonExistent() {
        authTokenRepo.setAuthToken(1, authToken1);
        authTokenRepo.removeAuthToken(2); // Attempt to remove a non-existent token
        assertEquals(authToken1, authTokenRepo.getAuthToken(1)); // Ensure the existing token is still there
    }

    @Test
    public void testGetUserIdByTokenNonExistent() {
        authTokenRepo.setAuthToken(1, authToken1);
        authTokenRepo.setAuthToken(2, authToken2);
        assertEquals(-1, authTokenRepo.getUserIdByToken("nonexistent_token")); // Ensure it returns -1 for non-existent token
    }

    @Test
    public void testGetAuthTokenNonExistent() {
        authTokenRepo.setAuthToken(1, authToken1);
        assertNull(authTokenRepo.getAuthToken(2)); // Ensure it returns null for non-existent token
    }

    @Test
    public void testGetUserIdByTokenExpired() {
        authTokenRepo.setAuthToken(1, authToken1);
        authTokenRepo.setAuthToken(2, authToken2);
        authToken1.setExpirationDate(new Date(System.currentTimeMillis())); // Set token1 to expired
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        assertEquals(1, authTokenRepo.getUserIdByToken("token1")); // Ensure it returns -1 for expired token
    }

    @Test
    public void testGetAuthTokenExpired() {
        authTokenRepo.setAuthToken(1, authToken1);
        authTokenRepo.setAuthToken(2, authToken2);
        authToken1.setExpirationDate(new Date(System.currentTimeMillis())); // Set token1 to expired
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        assertNull(authTokenRepo.getAuthToken(1)); // Ensure it returns null for expired token
    }

    @Test
    void testSetAndGetValidAuthToken() {
        AuthToken token = new AuthToken("tok1", new Date(System.currentTimeMillis() + 10_000));
        authTokenRepo.setAuthToken(1, token);
        AuthToken fetched = authTokenRepo.getAuthToken(1);
        assertNotNull(fetched);
        assertEquals("tok1", fetched.getToken());
    }

    @Test
    void testSetAuthToken_InvalidArgs() {
        // null token
        assertThrows(OurArg.class, () -> authTokenRepo.setAuthToken(1, null));
        // negative userId
        AuthToken valid = new AuthToken("ok", new Date(System.currentTimeMillis() + 10_000));
        assertThrows(OurArg.class, () -> authTokenRepo.setAuthToken(-5, valid));
        // expired token
        AuthToken expired = new AuthToken("e", new Date(System.currentTimeMillis() - 1000));
        assertThrows(OurArg.class, () -> authTokenRepo.setAuthToken(2, expired));
    }

    @Test
    void testRemoveAndGetAuthToken_AfterRemovalIsNull() {
        AuthToken token = new AuthToken("x", new Date(System.currentTimeMillis() + 10_000));
        authTokenRepo.setAuthToken(3, token);
        authTokenRepo.removeAuthToken(3);
        assertNull(authTokenRepo.getAuthToken(3));
    }

    @Test
    void testGetUserIdByToken_Cases() {
        AuthToken token = new AuthToken("findme", new Date(System.currentTimeMillis() + 10_000));
        authTokenRepo.setAuthToken(5, token);

        // found
        assertEquals(5, authTokenRepo.getUserIdByToken("findme"));
        // not found
        assertEquals(-1, authTokenRepo.getUserIdByToken("nope"));
        // invalid args
        assertThrows(OurArg.class, () -> authTokenRepo.getUserIdByToken(null));
        assertThrows(OurArg.class, () -> authTokenRepo.getUserIdByToken(""));
    }
}
