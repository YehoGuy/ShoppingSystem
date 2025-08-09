package DTOs;

public class CartEntryDTO {
    private int shopId;
    private int quantity;
    private ItemDTO item;
    public CartEntryDTO() {}

    public CartEntryDTO(int quantity, ItemDTO item, int shopId) {
        this.shopId = shopId;
        this.quantity = quantity;
        this.item = item;
    }


    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public ItemDTO getItem() { return item; }
    public void setItem(ItemDTO item) { this.item = item; }

    public int getShopId() { return shopId; }
    public void setShopId(int shopId) { this.shopId = shopId; }
}
