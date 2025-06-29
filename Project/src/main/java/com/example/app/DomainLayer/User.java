package com.example.app.DomainLayer;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.DomainLayer.Purchase.Address;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

@MappedSuperclass
public abstract class User {
    @Embedded
    protected ShoppingCart shoppingCart; // Shopping cart associated with the user
    @Transient
    protected PaymentMethod paymentMethod; // Payment method associated with the user

    private String paymentMethodString;

    @Embedded
    protected Address address; // Shipping address associated with the user

    public User() {
        this.shoppingCart = new ShoppingCart(); // Initialize the shopping cart
        this.paymentMethodString = "";
    }

    public User(int cartId) {
        this.shoppingCart = new ShoppingCart(); // Initialize the shopping cart
        this.paymentMethodString = "";
    }

    public User(int cartId, Address address) {
        this.shoppingCart = new ShoppingCart(); // Initialize the shopping cart
        this.address = address; // Set the user's shipping address
        this.paymentMethodString = "";
    }

    public ShoppingCart getShoppingCart() {
        shoppingCart.loadFromPersistentCollections();
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

    public void updateShoppingCartItemQuantity(int shopID, int itemID, boolean b) {
        if (!shoppingCart.hasItemOfShop(shopID, itemID)) {
            System.out.println("hola");
            throw new OurRuntime("item not in cart.", shopID, itemID);
        }
        int addOrRemove = b ? 1 : -1;
        shoppingCart.updateProductQuantity(shopID, itemID, addOrRemove);
    }

    public void removeShoppingCartItem(int shopID, int itemID) {
        if (!shoppingCart.hasItemOfShop(shopID, itemID))
            throw new OurRuntime("item not in cart.", shopID, itemID);
        shoppingCart.removeItemFromCart(shopID, itemID);
    }

}
