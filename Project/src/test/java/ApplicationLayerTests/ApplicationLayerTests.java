package ApplicationLayerTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DomainLayer.AuthToken;
import com.example.app.DomainLayer.IAuthTokenRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

class ApplicationLayerTests {
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

    // ----- OurArg & OurRuntime -----

    @Test
    void testOurArg_Constructors() {
        OurArg a1 = new OurArg("m");
        assertTrue(a1.getMessage().contains("IssacTheDebugException thrown! mesage: m"));

        OurArg a2 = new OurArg("m2", new RuntimeException("c"));
        assertTrue(a2.getMessage().contains("IssacTheDebugException thrown! message: m2"));

        OurArg a3 = new OurArg(new IllegalStateException("err"));
        assertTrue(a3.getMessage().contains("IssacTheDebugException caused by: IllegalStateException"));
    }

    @Test
    void testOurRuntime_Constructors() {
        OurRuntime r1 = new OurRuntime("msg");
        assertTrue(r1.getMessage().contains("MosheTheDebugException thrown! mesage: msg"));

        OurRuntime r2 = new OurRuntime("msg2", new IllegalArgumentException("cause"));
        assertTrue(r2.getMessage().contains("MosheTheDebugException thrown! message: msg2"));

        OurRuntime r3 = new OurRuntime(new NullPointerException());
        assertTrue(r3.getMessage().contains("MosheTheDebugException caused by: NullPointerException"));
    }
}
