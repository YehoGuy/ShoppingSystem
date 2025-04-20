package ApplicationLayerTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import ApplicationLayer.AuthTokenService;
import InfrastructureLayer.AuthTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class AuthServiceTests {
    
    private static final String SECRET_KEY = "testSecretKey";
    private static final long EXPIRATION_TIME = 864000000; // 10 days
    
    private AuthTokenService authService = new AuthTokenService(new AuthTokenRepository());

    @Test
    public void generateToken_ShouldCreateValidJWT() {
        String username = "testUser";
        
        String token = authService.generateAuthToken(username);
        
        assertNotNull(token);
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
                
        assertEquals(username, claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    public void generateToken_ShouldHaveCorrectExpiration() {
        String username = "testUser";
        
        String token = authService.generateAuthToken(username);
        
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
                
        long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(EXPIRATION_TIME, diff);
    }

    @Test
    public void validateToken_ShouldReturnTrueForValidToken() {
        String username = "testUser";
        String token = authService.generateAuthToken(username);
        
        int isValid = authService.ValidateToken(token);
        
        assertNotEquals(-1, isValid);
    }

    @Test
    public void validateToken_ShouldReturnFalseForInvalidToken() {
        String invalidToken = "invalidToken";
        
        int isValid = authService.ValidateToken(invalidToken);
        
        assertEquals(-1, isValid);
    }
}