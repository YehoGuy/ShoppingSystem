package DomainLayer.Shop;

import java.util.Map;
import java.util.Set;

/**
 * A discount that applies a percentage off the entire purchase
 * if all specified bundle item IDs are present in the cart.
 */
public class BundleDiscount implements Discount {
    private final int shopId;
    private final Set<Integer> bundleItems;
    private int percentage;

    /**
     * @param shopId       the shop this discount belongs to
     * @param bundleItems  the set of item IDs that must all be in the cart
     * @param percentage   the discount percentage (0–100)
     */
    public BundleDiscount(int shopId, Set<Integer> bundleItems, int percentage) {
        if (bundleItems == null || bundleItems.isEmpty()) {
            throw new IllegalArgumentException("Bundle items must not be null or empty");
        }
        validatePercentage(percentage);
        this.shopId       = shopId;
        this.bundleItems  = Set.copyOf(bundleItems);
        this.percentage   = percentage;
    }

    @Override
    public int getShopId() {
        return shopId;
    }

    @Override
    public Integer getItemId() {
        return null;  // whole‐cart discount when bundle is present
    }

    @Override
    public int getPercentage() {
        return percentage;
    }

    @Override
    public void setPercentage(int percentage) {
        validatePercentage(percentage);
        this.percentage = percentage;
    }

    @Override
    public int applyDiscounts(Map<Integer, Integer> items, int totalPrice) {
        if (items.keySet().containsAll(bundleItems)) {
            return totalPrice * (100 - percentage) / 100;
        }
        return totalPrice;
    }

    private void validatePercentage(int p) {
        if (p < 0 || p > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
    }
}
