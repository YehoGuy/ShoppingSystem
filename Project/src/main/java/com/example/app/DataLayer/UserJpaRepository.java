package com.example.app.DataLayer;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.User;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    // Additional query methods can be defined here if needed

}
