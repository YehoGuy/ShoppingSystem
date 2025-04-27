package DomainLayer.Shop;

import java.util.Map;

/**
 * A discount that applies a percentage off the entire purchase
 * once the total quantity of all items exceeds a given threshold.
 */
public class QuantityThresholdDiscount implements Discount {
    private final int shopId;
    private final int threshold;
    private int percentage;

    /**
     * @param shopId      the shop this discount belongs to
     * @param threshold   the total‐quantity threshold to trigger the discount
     * @param percentage  the discount percentage (0–100)
     */
    public QuantityThresholdDiscount(int shopId, int threshold, int percentage) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must be non-negative");
        }
        validatePercentage(percentage);
        this.shopId     = shopId;
        this.threshold  = threshold;
        this.percentage = percentage;
    }

    @Override
    public int getShopId() {
        return shopId;
    }

    @Override
    public Integer getItemId() {
        return null;  // whole‐cart discount
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
        int totalQty = items.values().stream().mapToInt(Integer::intValue).sum();
        if (totalQty > threshold) {
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
