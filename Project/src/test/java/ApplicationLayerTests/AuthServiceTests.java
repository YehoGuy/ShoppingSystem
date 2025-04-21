package ApplicationLayerTests;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import ApplicationLayer.AuthTokenService;
import InfrastructureLayer.AuthTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class AuthServiceTests {
    
    private AuthTokenService authService = new AuthTokenService(new AuthTokenRepository());

    private static final long EXPIRATION_TIME = 86400000; 
    private SecretKey key = authService.getKey();
    

    @Test
    public void testGenerateToken() {
        String username = "testUser";
        
        String token = authService.generateAuthToken(username);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
                
        assertEquals(username, claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    public void testExpirationTime() {
        String username = "testUser";
        
        String token = authService.generateAuthToken(username);
        
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(EXPIRATION_TIME, diff);
    }

    @Test
    public void testValidateToken() {
        String username = "testUser";
        String token = authService.Login(username, username, 1);
        
        Integer isValid = authService.ValidateToken(token);
        
        assertNotNull(isValid);
    }

    @Test
    public void testInvalidToken() {
        String invalidToken = "invalidToken";
        
        Integer isValid = authService.ValidateToken(invalidToken);
        
        assertNull(isValid);
    }
}