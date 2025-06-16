package DBLayerTests;
/* 
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.app.DomainLayer.AuthToken;
import com.example.app.DomainLayer.IAuthTokenRepository;
import com.example.app.SimpleHttpServerApplication;

import jakarta.transaction.Transactional;

@SpringBootTest(classes = SimpleHttpServerApplication.class)
@ActiveProfiles({ "db-test" })
@Transactional
public class AuthTokenRepositoryDBImplTests {

    @Autowired
    private IAuthTokenRepository authTokenRepo;

    private AuthToken createToken(int memberId) {
        String token = UUID.randomUUID().toString();
        Date expiry = new Date(System.currentTimeMillis() + 3600 * 1000); // 1 hour in future
        AuthToken authToken = new AuthToken(token, expiry, memberId);
        authToken.setUserId(memberId);
        return authToken;
    }

    @Test
    public void testInsertAndRetrieveToken() {
        AuthToken token = createToken(123);
        authTokenRepo.setAuthToken(123, token);

        AuthToken retrieved = authTokenRepo.getAuthToken(123);
        assertNotNull(retrieved);
        assertEquals(token.getToken(), retrieved.getToken());
    }

    @Test
    public void testDeleteToken() {
        AuthToken token = createToken(456);
        authTokenRepo.setAuthToken(456, token);

        AuthToken before = authTokenRepo.getAuthToken(456);
        assertNotNull(before);

        authTokenRepo.removeAuthToken(456);

        AuthToken after = authTokenRepo.getAuthToken(456);
        assertNull(after);
    }

    @Test
    public void testGetUserIdByToken() {
        AuthToken token = createToken(789);
        authTokenRepo.setAuthToken(789, token);

        int userId = authTokenRepo.getUserIdByToken(token.getToken());
        assertEquals(789, userId);
    }

    @Test
    public void testGetUserIdByInvalidToken() {
        int userId = authTokenRepo.getUserIdByToken("nonexistent-token");
        assertEquals(-1, userId);
    }

    @Test
    public void testTokenExpirationLogic() {
        Date expired = new Date(System.currentTimeMillis() - 1000); // 1 sec ago
        AuthToken expiredToken = new AuthToken("expired", expired, 10);
        assertTrue(expiredToken.isExpired());

        Date valid = new Date(System.currentTimeMillis() + 10000); // 10 sec future
        AuthToken validToken = new AuthToken("valid", valid, 10);
        assertFalse(validToken.isExpired());
    }
}
*/