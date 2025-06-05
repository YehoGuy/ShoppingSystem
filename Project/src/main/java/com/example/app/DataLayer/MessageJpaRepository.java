package com.example.app.DataLayer;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.Message;

public interface MessageJpaRepository extends JpaRepository<Message, Long> {
    // Additional query methods can be defined here if needed

}
