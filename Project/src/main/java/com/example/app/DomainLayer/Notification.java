package com.example.app.DomainLayer;

import jakarta.persistence.Embeddable;

@Embeddable
public class Notification {

    private String title;
    private String message;

    public Notification(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public Notification() {
        this.title = ""; // Default title
        this.message = ""; // Default message
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
        return title + '\n' + message;
    }

}
