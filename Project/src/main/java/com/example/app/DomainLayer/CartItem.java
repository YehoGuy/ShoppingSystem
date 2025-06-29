package com.example.app.DomainLayer;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

@Embeddable
public class CartItem {
    @Column(name = "shop_id")
    private Integer shopId;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "quantity")
    private Integer quantity;

    public CartItem() {
    }

    public CartItem(Integer shopId, Integer productId, Integer quantity) {
        this.shopId = shopId;
        this.productId = productId;
        this.quantity = quantity;
    }

    // Getters and setters
    public Integer getShopId() {
        return shopId;
    }

    public void setShopId(Integer shopId) {
        this.shopId = shopId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CartItem cartItem = (CartItem) obj;
        return shopId != null ? shopId.equals(cartItem.shopId)
                : cartItem.shopId == null &&
                        productId != null ? productId.equals(cartItem.productId) : cartItem.productId == null;
    }

    @Override
    public int hashCode() {
        int result = shopId != null ? shopId.hashCode() : 0;
        result = 31 * result + (productId != null ? productId.hashCode() : 0);
        return result;
    }

    // return false is quantity = 0
    public boolean updateItemQuantity(int addOrRemove) {
        this.quantity += addOrRemove;
        if (quantity == 0)
            return false;
        return true;
    }
}
