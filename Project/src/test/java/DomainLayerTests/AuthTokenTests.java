package DomainLayerTests;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DomainLayer.AuthToken;

public class AuthTokenTests {
    private static AuthToken authToken;
    private static Date expirationDate; 
    @BeforeEach
    public static void setup() {
        expirationDate = new Date(System.currentTimeMillis()+1000*60*60); 
        authToken = new AuthToken("token",expirationDate); 
        
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
}