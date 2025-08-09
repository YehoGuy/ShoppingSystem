package DomainLayerTests;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.AuthToken;

public class AuthTokenTests {
    private AuthToken authToken;
    private Date expirationDate; 
    @BeforeEach
    public void setup() {
        expirationDate = new Date(System.currentTimeMillis()+1000*60*60); 
        authToken = new AuthToken("token",expirationDate, 10); 
        
    }

    @Test
    public void testGetToken() {
        assertEquals("token", authToken.getToken());
    }

    @Test
    public void testGetExpirationDate() {
        assertEquals(expirationDate, authToken.getExpirationTime());
    }

    @Test
    public void testIsExpired() throws InterruptedException {
        assertFalse(authToken.isExpired()); 
        authToken.setExpirationDate(new Date(System.currentTimeMillis())); 
        Thread.sleep(1000);
        assertTrue(authToken.isExpired());
    }

    @Test
    public void testGetUserId() {
        assertEquals(10, authToken.getUserId());
    }

    @Test
    public void testSetUserId() {
        authToken.setUserId(20);
        assertEquals(20, authToken.getUserId());
    }

    @Test
    public void testConstructor() {
        AuthToken newAuthToken = new AuthToken("newToken", new Date(), 20);
        assertEquals("newToken", newAuthToken.getToken());
        assertEquals(20, newAuthToken.getUserId());
        assertTrue(newAuthToken.getExpirationTime().after(new Date(0))); // Ensure expiration
        assertTrue(newAuthToken.getExpirationTime().before(new Date(System.currentTimeMillis() + 1000 * 60 * 60))); // Ensure expiration is in the future
    }


}