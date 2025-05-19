package ApplicationLayerTests;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.DomainLayer.AuthToken;
import com.example.app.DomainLayer.IAuthTokenRepository;
import com.example.app.InfrastructureLayer.AuthTokenRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class AuthServiceTests {
    
    private AuthTokenService authService = new AuthTokenService(new AuthTokenRepository());

    private static final long EXPIRATION_TIME = 86400000; 
    private SecretKey key = authService.getKey();

    @Mock
    private IAuthTokenRepository authTokenRepository;

    @InjectMocks
    private AuthTokenService authTokenService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }
    

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
        
        Integer isValid;
        try {
            isValid = authService.ValidateToken(token);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            isValid = null; // Set to null if an exception occurs
        }
        
        assertNotNull(isValid);
    }

    @Test
    public void testInvalidToken() {
        String invalidToken = "invalidToken";
        
        Integer isValid;
        try {
            isValid = authService.ValidateToken(invalidToken);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            isValid = null; // Set to null if an exception occurs
        }
        
        assertNull(isValid);
    }

     // ----- AuthTokenService: AuthenticateGuest -----

    @Test
    void testAuthenticateGuest_Success() {
        doNothing().when(authTokenRepository).setAuthToken(eq(1), any(AuthToken.class));
        String token = authTokenService.AuthenticateGuest(1);
        assertNotNull(token);
        verify(authTokenRepository).setAuthToken(eq(1), any(AuthToken.class));
    }

    @Test
    void testAuthenticateGuest_InvalidGuestId() {
        OurArg ex = assertThrows(OurArg.class, () -> authTokenService.AuthenticateGuest(0));
        assertTrue(ex.getMessage().contains("Guest ID must be a positive integer"));
    }

    // ----- AuthTokenService: Login -----

    @Test
    void testLogin_Success() {
        doNothing().when(authTokenRepository).setAuthToken(eq(2), any(AuthToken.class));
        String token = authTokenService.Login("user", "pass", 2);
        assertNotNull(token);
        verify(authTokenRepository).setAuthToken(eq(2), any(AuthToken.class));
    }

    @Test
    void testLogin_InvalidUsername() {
        assertThrows(OurArg.class, () -> authTokenService.Login(null, "p", 1));
        assertThrows(OurArg.class, () -> authTokenService.Login("", "p", 1));
    }

    @Test
    void testLogin_InvalidPassword() {
        assertThrows(OurArg.class, () -> authTokenService.Login("u", null, 1));
        assertThrows(OurArg.class, () -> authTokenService.Login("u", "", 1));
    }

    @Test
    void testLogin_InvalidUserId() {
        assertThrows(OurArg.class, () -> authTokenService.Login("u", "p", 0));
    }

    // ----- AuthTokenService: generateAuthToken -----

    @Test
    void testGenerateAuthToken_Success() {
        String token = authTokenService.generateAuthToken("bob");
        assertNotNull(token);
        // must be parseable
        String subject = Jwts.parserBuilder()
            .setSigningKey(authTokenService.getKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
        assertEquals("bob", subject);
    }

    @Test
    void testGenerateAuthToken_InvalidUsername() {
        assertThrows(OurArg.class, () -> authTokenService.generateAuthToken(null));
        assertThrows(OurArg.class, () -> authTokenService.generateAuthToken(""));
    }

    // ----- AuthTokenService: extractUsername & extractExpiration -----

    @Test
    void testExtractUsernameAndExpiration() {
        String token = authTokenService.generateAuthToken("alice");
        String name = authTokenService.extractUsername(token);
        assertEquals("alice", name);
        Date exp = authTokenService.extractExpiration(token);
        assertTrue(exp.after(new Date()));
    }

    @Test
    void testExtractUsername_InvalidToken() {
        assertThrows(OurArg.class, () -> authTokenService.extractUsername(null));
        assertThrows(OurArg.class, () -> authTokenService.extractUsername(""));
    }

    @Test
    void testExtractExpiration_InvalidToken() {
        assertThrows(OurArg.class, () -> authTokenService.extractExpiration(null));
        assertThrows(OurArg.class, () -> authTokenService.extractExpiration(""));
    }

    // ----- AuthTokenService: ValidateToken -----

    @Test
    void testValidateToken_Success() throws Exception {
        String token = authTokenService.generateAuthToken("joe");
        when(authTokenRepository.getUserIdByToken(token)).thenReturn(5);
        Integer uid = authTokenService.ValidateToken(token);
        assertEquals(5, uid);
    }

    @Test
    void testValidateToken_NotFound() {
        String token = authTokenService.generateAuthToken("joe");
        when(authTokenRepository.getUserIdByToken(token)).thenReturn(-1);
        Exception ex = assertThrows(Exception.class, () -> authTokenService.ValidateToken(token));
        assertTrue(ex.getMessage().contains("Token not found in repository"));
    }

    @Test
    void testValidateToken_Expired() throws Exception {
        // build an already‐expired JWT
        SecretKey key = authTokenService.getKey();
        String expired = Jwts.builder()
            .setSubject("user")
            .setIssuedAt(new Date(System.currentTimeMillis() - 2000))
            .setExpiration(new Date(System.currentTimeMillis() - 1000))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

        when(authTokenRepository.getUserIdByToken(expired)).thenReturn(7);
        Exception ex = assertThrows(Exception.class, () -> authTokenService.ValidateToken(expired));
        assertTrue(ex.getMessage().contains("Token expired"));
    }


    @Test
    void testValidateToken_InvalidSignature() {
        // random string isn't a JWT
        Exception ex = assertThrows(Exception.class, () -> authTokenService.ValidateToken("bad.token"));
        assertTrue(ex.getMessage().contains("Invalid token"));
    }

    @Test
    void testValidateToken_NullOrEmpty() {
        assertThrows(OurArg.class, () -> authTokenService.ValidateToken(null));
        assertThrows(OurArg.class, () -> authTokenService.ValidateToken(""));
    }

    // ----- AuthTokenService: Logout -----

    @Test
    void testLogout_Success() throws Exception {
        // arrange
        String token = authTokenService.generateAuthToken("sam");
        when(authTokenRepository.getUserIdByToken(token)).thenReturn(8);

        // create a spy so we can stub ValidateToken(...)
        AuthTokenService svc = spy(authTokenService);
        doReturn(8).when(svc).ValidateToken(token);

        // stub the repository void method
        doNothing().when(authTokenRepository).removeAuthToken(8);

        // act
        svc.Logout(token);

        // assert
        verify(authTokenRepository).removeAuthToken(8);
    }


    @Test
    void testLogout_ValidateFails() {
        String t = "x";
        assertThrows(Exception.class, () -> authTokenService.Logout(t));
    }
}