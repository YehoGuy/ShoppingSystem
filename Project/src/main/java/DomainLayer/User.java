package DomainLayer;

public abstract class User {
    private ShoppingCart shoppingCart; // Shopping cart associated with the user

    public User(int cartId) {
        this.shoppingCart = new ShoppingCart(); // Initialize the shopping cart
    }

    public ShoppingCart getShoppingCart() {
        return shoppingCart; // Return the user's shopping cart
    }

    public void setShoppingCart(ShoppingCart shoppingCart) {
        this.shoppingCart = shoppingCart; // Set a new shopping cart for the user
    }

    public void mergeShoppingCart(ShoppingCart otherCart) {
        if (otherCart != null) {
            this.shoppingCart.mergeCart(otherCart); // Merge the items from the other cart into this user's cart
        }
    }


}
