package com.example.app.PresentationLayer.DTO.User;


import com.example.app.PresentationLayer.DTO.Purchase.AddressDTO;

import jakarta.validation.constraints.Positive;

public record GuestDTO(
        @Positive int guestId,
        ShoppingCartDTO shoppingCart,
        AddressDTO address) {

    /* -------- Domain ➜ DTO -------- */
    public static GuestDTO fromDomain(com.example.app.DomainLayer.Guest g,
                                      ShoppingCartDTO shoppingCart) {
        return new GuestDTO(
                g.getGuestId(),
                shoppingCart,
                g.getAddress() != null ? AddressDTO.fromDomain(g.getAddress()) : null);
    }

    /* -------- DTO ➜ Domain (rare, mostly tests) -------- */
    public com.example.app.DomainLayer.Guest toDomain() {
        com.example.app.DomainLayer.Guest g = new com.example.app.DomainLayer.Guest(guestId);
        if (shoppingCart != null)  g.setShoppingCart(shoppingCart.toDomain());
        if (address != null)       g.setAddress(address.toDomain());
        return g;
    }
}