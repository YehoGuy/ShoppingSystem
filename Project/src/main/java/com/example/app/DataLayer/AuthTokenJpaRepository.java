package com.example.app.DataLayer;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.AuthToken;

public interface AuthTokenJpaRepository extends JpaRepository<AuthToken, Long> {
    // Additional query methods can be defined here if needed

}
