package com.example.app.DBLayer.Message;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.IMessageRepository;
import com.example.app.DomainLayer.Message;

@Profile("!no-db & !test")
public interface MessageRepositoryDB extends JpaRepository<Message, Integer>, IMessageRepository{
    // This interface extends JpaRepository to provide CRUD operations for Message
    // entities.
}
