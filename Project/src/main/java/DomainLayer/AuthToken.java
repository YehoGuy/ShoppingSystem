package DomainLayer;

import java.util.Date;

public class AuthToken {
    private String token; // The authentication token string
    private Date expirationTime; // The time when the token expires

    public AuthToken(String token, Date expirationTime) {
        this.token = token;
        this.expirationTime = expirationTime;
    }

    public String getToken() {
        return token;
    }

    public Date getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationDate(Date expirationTime) {
        this.expirationTime = expirationTime;
    }

    public boolean isExpired() {
        return new Date().after(expirationTime); // Check if the current date is after the expiration time
    }
}
