package com.example.app.InfrastructureLayer;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderUtil {

    private boolean isTest;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // Use BCrypt for password encoding

    public PasswordEncoderUtil() {
        this.isTest = false; // Default to production mode
    }

    public void setIsTest(boolean isTest) {
        this.isTest = isTest; // Set the mode to test or production
    }

    public String encode(String password) {
        if (isTest) {
            return password; // In test mode, return the raw password without encoding
        }
        return passwordEncoder.encode(password); // Encode the password using BCrypt
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        if (isTest) {
            return rawPassword.equals(encodedPassword); // In test mode, compare raw passwords directly
        }
        return passwordEncoder.matches(rawPassword, encodedPassword); // Check if the raw password matches the encoded password
    }
    
}
