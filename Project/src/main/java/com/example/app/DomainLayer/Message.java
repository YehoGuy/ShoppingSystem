package com.example.app.DomainLayer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer messageId;

    private final int senderId;

    private final int receiverId;

    private final String content;

    private final String timestamp;

    private final boolean userToUser;

    private final int previousMessageId;

    private boolean isDeleted; // Flag to indicate if the message is deleted

    // JPA requires a no-arg constructor
    public Message() {
        this.senderId = -1;
        this.receiverId = -1;
        this.content = "null";
        this.timestamp = "null";
        this.userToUser = false;
        this.previousMessageId = -1;
        this.isDeleted = false;
    }

    public Message(int messageId, int senderId, int receiverId, String content, String timestamp, boolean userToUser,
            int previousMessageId) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.userToUser = userToUser;
        this.previousMessageId = previousMessageId;
        this.isDeleted = false;
    }

    public Message(int senderId, int receiverId, String content, String timestamp, boolean userToUser,
            int previousMessageId) {
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
