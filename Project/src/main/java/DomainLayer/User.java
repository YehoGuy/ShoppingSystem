import DomainLayer.ShoppingCart; // Importing the ShoppingCart class

public abstract class User {
    final private ShoppingCart shoppingCart; // Shopping cart associated with the user

    public User(int cartId) {
        this.shoppingCart = new ShoppingCart(); // Initialize the shopping cart
    }

    public ShoppingCart getShoppingCart() {
        return shoppingCart; // Return the user's shopping cart
    }
}
