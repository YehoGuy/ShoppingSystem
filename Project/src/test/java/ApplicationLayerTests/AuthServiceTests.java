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
        // Arrange
        String username = "testUser";
        
        // Act
        String token = authService.generateAuthToken(username);
        
        // Assert
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
        // Arrange
        String username = "testUser";
        
        // Act
        String token = authService.generateAuthToken(username);
        
        // Assert
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
                
        long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(EXPIRATION_TIME, diff);
    }

    @Test
    public void validateToken_ShouldReturnTrueForValidToken() {
        // Arrange
        String username = "testUser";
        String token = authService.generateAuthToken(username);
        
        // Act
        int isValid = authService.ValidateToken(token);
        
        // Assert
        assertNotEquals(-1, isValid);
    }

    @Test
    public void validateToken_ShouldReturnFalseForInvalidToken() {
        // Arrange
        String invalidToken = "invalidToken";
        
        // Act
        int isValid = authService.ValidateToken(invalidToken);
        
        // Assert
        assertEquals(-1, isValid);
    }
}