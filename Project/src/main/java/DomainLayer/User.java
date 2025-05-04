package DomainLayer;

import ApplicationLayer.Purchase.PaymentMethod;
import DomainLayer.Purchase.Address;


public abstract class User {
    protected ShoppingCart shoppingCart; // Shopping cart associated with the user
    protected PaymentMethod paymentMethod; // Payment method associated with the user
    protected Address address; // Shipping address associated with the user

    public User(int cartId) {
        this.shoppingCart = new ShoppingCart(); // Initialize the shopping cart
    }

    public User(int cartId, Address address) {
        this.shoppingCart = new ShoppingCart(); // Initialize the shopping cart
        this.address = address; // Set the user's shipping address
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod; // Return the user's payment method
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod; // Set a new payment method for the user
    }

    public Address getAddress() {
        return this.address; // Return the user's shipping address
    }

    public void setAddress(Address Address) {
        this.address = Address; // Set a new shipping address for the user
    }

    public void setAddress(String country, String city, String street, int aparmentNum, String postalCode) {
        this.address = new Address().withCountry(country)
                                    .withCity(city)
                                    .withStreet(street)
                                    .withApartmentNumber(aparmentNum)
                                    .withZipCode(postalCode); 
    }

}
