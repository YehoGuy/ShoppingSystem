package com.example.app.DomainLayer;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.*;

@Entity
@Table(name = "auth_tokens")
public class AuthToken {

    @Id
    @Column(nullable = false, unique = true, length = 2048)
    private String token; // The authentication token string

    @Column(name = "expiry_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expirationTime; // The time when the token expires

    public AuthToken() {
        // Default constructor for JPA
    }

    public AuthToken(String token, Date expirationTime) {
        this.token = token;
        this.expirationTime = expirationTime;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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
