package DTOs;

import Domain.ItemCategory;

public class DiscountDTO {
    private Integer percentage;
    private boolean isDouble;
    private ItemCategory itemCategory; // null → not a category discount
    private Integer itemId;            // null → not an item discount
    private CompositePolicyDTO policy;

    public DiscountDTO() {}

    public DiscountDTO(Integer percentage, boolean isDouble,
                       ItemCategory itemCategory, Integer itemId) {
        this.percentage   = percentage;
        this.isDouble     = isDouble;
        this.itemCategory = itemCategory;
        this.itemId       = itemId;
    }

    public Integer getPercentage()    { return percentage; }
    public boolean isDouble()         { return isDouble; }
    public ItemCategory getItemCategory() { return itemCategory; }
    public Integer getItemId()        { return itemId; }

    public CompositePolicyDTO getPolicy()      { return policy; }
    public void setPolicy(CompositePolicyDTO policy) { this.policy = policy; }

    @Override
    public String toString() {
        String base;
        if (itemId != null && itemId != 0) {
            base = "Discount of " + percentage + "% on item with ID " + itemId;
        } else if (itemCategory != null) {
            base = "Discount of " + percentage + "% on items in category " + itemCategory;
        } else {
            base = "Discount of " + percentage + "% on the entire shop";
        }
        // ← INCLUDE policy text if present
        return base + (policy != null ? " | policy: " + policy.toString() : "");
    }
}
