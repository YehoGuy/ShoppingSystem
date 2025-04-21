package ApplicationLayer;

import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;

import DomainLayer.AuthToken;
import DomainLayer.IAuthTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class AuthTokenService {
    @Value("${jwt.secret}")
    private String secret; 
    private static final long EXPIRATION_TIME = 86400000; 
    private SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256); 

    private IAuthTokenRepository authTokenRepository; 
    // private UserService userService; 

    public AuthTokenService(IAuthTokenRepository authTokenRepository) {
        this.authTokenRepository = authTokenRepository; 
        // this.userService = userService; 
    }

    public SecretKey getKey() {
        return key; 
    }

    public String AuthenticateGuest(int guestId) {
        LoggerService.logMethodExecution("AuthenticateGuest",guestId);
        if(guestId <= 0) {
            throw new IllegalArgumentException("Guest ID must be a positive integer");
        }
        String token = generateAuthToken("guest");
        long expirationTime = System.currentTimeMillis() + EXPIRATION_TIME;
        AuthToken authToken = new AuthToken(token, new Date(expirationTime));
        authTokenRepository.setAuthToken(guestId, authToken);
        LoggerService.logMethodExecutionEnd("AuthenticateGuest",token);
        return token;
    }


    public String Login(String username, String password, int userId) {
        LoggerService.logMethodExecution("Login",username,password,userId);
        if(username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if(password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if(userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive integer");
        }
            String token = generateAuthToken(username); 
            long expirationTime = System.currentTimeMillis() + EXPIRATION_TIME; 
            AuthToken authToken = new AuthToken(token, new Date(expirationTime));
            int userId = 0;//userService.getUserIdByUsername(username);
            authTokenRepository.setAuthToken(userId, authToken);
            LoggerService.logMethodExecutionEnd("Login",token);
            return token;
    }


    public String Logout(String token) {
         LoggerService.logMethodExecution("Logout",token);
        
        if(ValidateToken(token) != null) { 
            int userId = authTokenRepository.getUserIdByToken(token); 
            authTokenRepository.removeAuthToken(userId); 
            LoggerService.logMethodExecutionEndVoid("Logout");
        } else {
            throw new Exception("Token not found in repository"); 
        }
    }

    public String generateAuthToken(String username) {
        LoggerService.logMethodExecution("generateAuthToken",username);
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
        LoggerService.logMethodExecutionEnd("generateAuthToken",token);
        return token;
    }


    public Integer ValidateToken(String token) throws Exception {
        LoggerService.logMethodExecution("ValidateToken",token);
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(key) 
                    .build()
                    .parseClaimsJws(token); 
            if(extractUsername(token) == null || extractUsername(token).isEmpty()) {
                throw new JwtException("Invalid token"); 
            }
            int userId = authTokenRepository.getUserIdByToken(token); 
            if (userId != -1) {
                if(extractExpiration(token).before(new Date())) {
                    authTokenRepository.removeAuthToken(authTokenRepository.getUserIdByToken(token));
                    throw new ExpiredJwtException(null, null, "Token expired"); 
                }
                LoggerService.logMethodExecutionEnd("ValidateToken",userId);
                return userId; 
            } else {
                return null; 
            }
        }
        catch (ExpiredJwtException e) {

            LoggerService.logError("ValidateToken", e, token);
            throw new Exception("Token expired");
        } catch (JwtException e) {
            LoggerService.logError("ValidateToken", e, token);
            throw new Exception("Invalid token"); 
        } catch (Exception e) {
            LoggerService.logError("ValidateToken", e, token);
            throw new Exception("Token validation failed: " + e.getMessage()); 

        }
    }

    public String extractUsername(String token) {
        LoggerService.logMethodExecution("extractUsername",token);
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        String username = extractClaim(token, Claims::getSubject);  
        LoggerService.logMethodExecutionEnd("extractUsername",username);
        return username;
    }

    public Date extractExpiration(String token) {
        LoggerService.logMethodExecution("extractExpiration",token);
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        Date expiration = extractClaim(token, Claims::getExpiration); 
        LoggerService.logMethodExecutionEnd("extractExpiration",expiration);
        return expiration;
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        LoggerService.logMethodExecution("extractClaim",token);
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        T claim = claimsResolver.apply(extractAllClaims(token)); 
        LoggerService.logMethodExecutionEnd("extractClaim",claim);
        return claim;
    }

    private Claims extractAllClaims(String token) {
        LoggerService.logMethodExecution("extractAllClaims",token);
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key) 
                .build()
                .parseClaimsJws(token)
                .getBody(); 
        LoggerService.logMethodExecutionEnd("extractAllClaims",claims);
        return claims;
    }
}
