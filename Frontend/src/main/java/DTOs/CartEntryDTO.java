package DTOs;

public class CartEntryDTO {
    private int itemId;
    private int quantity;
    private ItemDTO item; // optional, for display

    public CartEntryDTO() {}

    public CartEntryDTO(int itemId, int quantity, ItemDTO item) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.item = item;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public ItemDTO getItem() { return item; }
    public void setItem(ItemDTO item) { this.item = item; }
}
