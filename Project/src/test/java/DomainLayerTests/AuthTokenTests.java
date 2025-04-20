package DomainLayerTests;

import java.util.Date;
import DomainLayer.AuthToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthTokenTests {
    private AuthToken authToken;
    private Date expirationDate; // Set expiration date to 10 seconds in the future
    @BeforeEach
    public static void setup() {
        expirationDate = new Date(System.currentTimeMillis()+100*60*60); // Set expiration date to 10 seconds in the future
        authToken = new AuthToken("token",expirationDate); // Set expiration date to 10 seconds in the future
        
    }

    @Test
    public void testGetToken() {
        assertEquals("token", authToken.getToken());
    }

    @Test
    public void testGetExpirationDate() {
        assertEquals(Date, authToken.getExpirationDate());
    }

    @Test
    public void testIsExpired() {
        assertFalse(authToken.isExpired());
        authToken.setExpirationDate(new Date(System.currentTimeMillis())); // Set to past date
        assertTrue(authToken.isExpired());
    }
}