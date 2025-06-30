package com.example.app.DBLayer.Message;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.Message;

public interface MessageRepositoryDB extends JpaRepository<Message, Integer> {
    // This interface extends JpaRepository to provide CRUD operations for Message
    // entities.
    List<Message> findBySenderId(int senderId);

    List<Message> findByReceiverId(int receiverId);
}
