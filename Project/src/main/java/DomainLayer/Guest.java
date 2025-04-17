package DomainLayer;

import DomainLayer.User;

public class Guest extends User {
    final private int guestId; // Unique identifier for the guest user

    public Guest(int guestId) {
        this.guestId = guestId; // Initialize guest ID
    }

    public int getGuestId() {
        return guestId; // Return the guest ID
    }
    
}
