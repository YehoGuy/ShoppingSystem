package DomainLayer;

public class AuthToken {
    private String token; // The authentication token string
    private long expirationTime; // The time when the token expires

    public AuthToken(String token, long expirationTime) {
        this.token = token;
        this.expirationTime = expirationTime;
    }

    public String getToken() {
        return token;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}
