package com.example.app.DBLayer.Message;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.Message;

public interface MessageRepositoryDB extends JpaRepository<Message, Integer> {
    // This interface extends JpaRepository to provide CRUD operations for Message
    // entities.
}
