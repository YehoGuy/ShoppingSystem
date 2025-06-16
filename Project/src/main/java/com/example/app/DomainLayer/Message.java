package com.example.app.DomainLayer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_seq")
    @SequenceGenerator(name = "message_seq", sequenceName = "message_sequence", allocationSize = 1)
    @Column(name = "message_id", nullable = false, updatable = false)
    private final int messageId;

    @Column(name = "sender_id", nullable = false)
    private final int senderId;

    @Column(name = "receiver_id", nullable = false)
    private final int receiverId;

    @Column(name = "content", nullable = false, length = 1024)
    private final String content;

    @Column(name = "timestamp", nullable = false, length = 50)
    private final String timestamp;

    @Column(name = "user_to_user", nullable = false)
    private final boolean userToUser;

    @Column(name = "previous_message_id")
    private final int previousMessageId;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted; // Flag to indicate if the message is deleted
    

    // JPA requires a no-arg constructor
    public Message() {
        this.messageId = -1; // Default value, will be set by JPA
        this.senderId = -1;
        this.receiverId = -1;
        this.content = "null";
        this.timestamp = "null";
        this.userToUser = false;
        this.previousMessageId = -1;
        this.isDeleted = false;
    }

    public Message(int messageId, int senderId, int receiverId, String content, String timestamp, boolean userToUser, int previousMessageId) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.userToUser = userToUser;
        this.previousMessageId = previousMessageId;
        this.isDeleted = false;
    }

    public int getMessageId() {
        return messageId;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isUserToUser() {
        return userToUser;
    }

    public int getPreviousMessageId() {
        return previousMessageId;
    }

    public synchronized boolean isDeleted() {
        return isDeleted;
    }

    public void delete() {
        isDeleted = true;
    }

    @Override
    public String toString() {
        return "Message{" +
                ", senderId=" + senderId +
                ", content='" + content + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
