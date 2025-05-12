package PresentationLayer.DTO.User;


import jakarta.validation.constraints.Positive;
import PresentationLayer.DTO.Purchase.AddressDTO;

public record GuestDTO(
        @Positive int guestId,
        ShoppingCartDTO shoppingCart,
        AddressDTO address) {

    /* -------- Domain ➜ DTO -------- */
    public static GuestDTO fromDomain(DomainLayer.Guest g) {
        return new GuestDTO(
                g.getGuestId(),
                ShoppingCartDTO.fromDomain(g.getShoppingCart()),
                g.getAddress() != null ? AddressDTO.fromDomain(g.getAddress()) : null);
    }

    /* -------- DTO ➜ Domain (rare, mostly tests) -------- */
    public DomainLayer.Guest toDomain() {
        DomainLayer.Guest g = new DomainLayer.Guest(guestId);
        if (shoppingCart != null)  g.setShoppingCart(shoppingCart.toDomain());
        if (address != null)       g.setAddress(address.toDomain());
        return g;
    }
}