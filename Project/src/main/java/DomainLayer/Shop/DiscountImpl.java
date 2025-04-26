package DomainLayer.Shop;

import java.util.Map;

/**
 * Single implementation of {@link Discount} for both global and item-specific discounts.
 */
public class DiscountImpl implements Discount {
    private final int shopId;
    private final Integer itemId; // null for global
    private int priceItem; // item price for item-specific
    private int percentage;

    /**
     * Constructor for a global discount.
     *
     * @param shopId    the shop ID
     * @param itemId   null for global discount
     * @param priceItem the price of the item (for item-specific discounts)
     * @param percentage the discount percentage (0-100)
     */
    public DiscountImpl(int shopId, Integer itemId, int priceItem, int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
        this.shopId = shopId;
        this.itemId = itemId;
        this.priceItem = priceItem;
        this.percentage = percentage;
    }


    @Override
    public int getShopId() {
        return shopId;
    }

    @Override
    public Integer getItemId() {
        return itemId;
    }

    @Override
    public int getPercentage() {
        return percentage;
    }

    @Override
    public void setPercentage(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
        this.percentage = percentage;
    }

    @Override
    public int applyDiscounts(Map<Integer, Integer> items, int totalPrice) {
        if (itemId == null) {
            // global: apply to entire cart
            return totalPrice * (100 - percentage) / 100;
        }
        // item-specific: calculate discount only on that item
        Integer qty = items.get(itemId);
        if (qty == null || qty <= 0) {
            return totalPrice;
        }
        // determine full price of that item (requires external price lookup)
        int itemPrice = priceItem;
        int fullItemTotal = itemPrice * qty;
        int discountedItemTotal = fullItemTotal * (100 - percentage) / 100;
        // rest of cart at full price:
        return (totalPrice - fullItemTotal) + discountedItemTotal;
    }

    public void setPriceItem(int priceItem) {
        if (priceItem < 0)
            throw new IllegalArgumentException("Price must be non-negative");
        this.priceItem = priceItem;
    }
}