package ApplicationLayer;

import java.util.Date;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.SecretKey;
import javax.swing.JComboBox;

import DomainLayer.IAuthTokenRepository;
import DomainLayer.IUserRepository;
import DomainLayer.AuthToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class AuthTokenService {
    @Value("${jwt.secret}")
    private String secret; 
    private static final long EXPIRATION_TIME = 86400000; 
    private SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256); 

    private IAuthTokenRepository authTokenRepository; 
    private IUserRepository userRepository; 

    public AuthTokenService(IAuthTokenRepository authTokenRepository, IUserRepository userRepository) {
        this.authTokenRepository = authTokenRepository; 
        this.userRepository = userRepository; 
    }

    public String Login(String username, String password) {
        if(userRepository.validateUser(username, password)) {
            String token = generateAuthToken(); 
            long expirationTime = System.currentTimeMillis() + EXPIRATION_TIME; 
            AuthToken authToken = new AuthToken(token,expirationTime);
            int userId = userRepository.getUserIdByUsername(username);
            authTokenRepository.setAuthToken(userId, authToken);
            return token;
        } else {
            return null;
        }
        
    }

    public String Logout(String token) {
        if(ValidateToken(token) != null) { 
            int userId = authTokenRepository.getUserIdByToken(token); 
            authTokenRepository.removeAuthToken(userId); 
            return "Logout successful"; 
        } else {
            return "Invalid token"; 
        }
    }

    private String generateAuthToken() {
        return Jwts.builder()
                .setSubject("user")
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public Integer ValidateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key) 
                    .build()
                    .parseClaimsJws(token); 
            int userId = authTokenRepository.getUserIdByToken(token); 
            if (userId != -1) {
                return userId; 
            } else {
                return null; 
            }
        }
        catch (ExpiredJwtException e) {
            return null; 
        } catch (JwtException e) {
            return null; 
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);  
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration); 
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token); 
        return claimsResolver.apply(claims); 
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key) 
                .build()
                .parseClaimsJws(token) 
                .getBody(); 
    }
}
