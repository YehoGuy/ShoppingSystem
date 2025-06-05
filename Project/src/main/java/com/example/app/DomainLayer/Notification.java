package com.example.app.DomainLayer;

import jakarta.persistence.*;

@Entity
@Table(name = "notifications")
public class Notification {
    /////////yes or no?
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private int notificationId; // Unique identifier for the notification
    ////////////
    
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false)
    private String message;

    public Notification(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "title='" + title + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
    
}
