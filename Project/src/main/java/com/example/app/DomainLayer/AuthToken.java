package com.example.app.DomainLayer;

import java.util.Date;

import jakarta.persistence.*;

@Entity
@Table(name = "authTokens")
public class AuthToken {

    @Id
    private String token; // The authentication token string

    private Date expirationTime; // The time when the token expires

    //TODO: what here?
    // @ManyToOne
    // @JoinColumn(name = "user_id", nullable = false)
    private int userId;


    protected AuthToken() {
        // JPA needs a default constructor
    }

    public AuthToken(String token, Date expirationTime, int userId) {
        this.token = token;
        this.expirationTime = expirationTime;
        this.userId = userId;
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


    //for testing 
    public int getUserId(){
        return userId;
    }

    public void setUserId(int memberId) {
        this.userId = memberId;
    }

}
