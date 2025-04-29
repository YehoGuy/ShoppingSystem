package DomainLayer.Shop.Discount;
 
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CategoryDiscount implements Discount {

    @Override
    public Integer getItemId() {
        return null; // Global discount, not item-specific
    }

    @Override
    public int getPercentage() {
        return 0; // Placeholder, should be set via constructor or method
    }

    @Override
    public void setPercentage(int percentage) {
        // Placeholder, should be implemented to set the discount percentage
    }

    @Override
    public Map<Integer, Integer> applyDiscounts(Map<Integer, Integer> items, Map<Integer, AtomicInteger> prices, Map<Integer, Integer> itemsDiscountedPrices) {
        // Placeholder for applying category-specific discounts
        return null; // Should return the updated prices after applying the discount
    }

}
