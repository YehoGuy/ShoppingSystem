
public class Guest extends User {
    final private int guestId; // Unique identifier for the guest user

    public Guest(int cartId, int guestId) {
        super(cartId); // Call the constructor of the User class
        this.guestId = guestId; // Initialize guest ID
    }

    public int getGuestId() {
        return guestId; // Return the guest ID
    }
    
}
