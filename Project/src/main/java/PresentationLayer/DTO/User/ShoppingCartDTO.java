package PresentationLayer.DTO.User;


import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 
Serialises the nested map <shopId → (itemId → quantity)>.*/
public record ShoppingCartDTO(
        @NotNull Map<Integer, Map<Integer, Integer>> items) {

    /* -------- Domain ➜ DTO (for responses) -------- */
    public static ShoppingCartDTO fromDomain(DomainLayer.ShoppingCart cart) {
        Map<Integer, Map<Integer, Integer>> copy = new java.util.HashMap<>();
        cart.getItems().forEach((shopId, basket) ->
                copy.put(shopId, new java.util.HashMap<>(basket)));
        return new ShoppingCartDTO(copy);
    }

    /* -------- DTO ➜ Domain (for create / merge) -------- */
    public DomainLayer.ShoppingCart toDomain() {
        DomainLayer.ShoppingCart cart = new DomainLayer.ShoppingCart();
    
        // Build the exact type HashMap<Integer, HashMap<Integer,Integer>>
        HashMap<Integer, HashMap<Integer, Integer>> cartMap = new HashMap<>();
        items.forEach((shopId, basket) ->       // basket is Map<Integer,Integer>
            cartMap.put(shopId, new HashMap<>(basket)));
    
        cart.restoreCart(cartMap);              // now matches the signature
        return cart;
    }
}