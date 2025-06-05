package com.example.app.DomainLayer;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("Guest")
public class Guest extends User {
    final private int guestId; // Unique identifier for the guest user

    public Guest(int guestId) {
        super(guestId); // Call the User class constructor with userName
        this.guestId = guestId; // Initialize guest ID
    }

    public int getGuestId() {
        return guestId; // Return the guest ID
    }
    
}
