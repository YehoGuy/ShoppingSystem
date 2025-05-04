package DomainLayer.Shop.Discount;
 
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import DomainLayer.Item.ItemCategory;

public class CategoryDiscount implements Discount {

    private final ItemCategory itemCategory; 
    private final int percentage;
    private Policy policyHead;
    private boolean isDouble;


    public CategoryDiscount(ItemCategory itemCategory, int percentage,Policy policyHead, boolean isDouble) {
        validatePercentage(percentage);
        this.itemCategory = itemCategory;
        this.percentage = percentage;
        this.policyHead = policyHead;
        this.isDouble = isDouble;
    }

    @Override
    public Map<Integer, Double> applyDiscounts(Map<Integer, Integer> items, Map<Integer, AtomicInteger> prices, Map<Integer, Double> itemsDiscountedPrices, Map<Integer, ItemCategory> itemsCategory) {
        if (!(checkPolicies(items, itemsDiscountedPrices, itemsCategory))) {
            return itemsDiscountedPrices;
        }
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            Integer itemId = entry.getKey();
            Integer qty = entry.getValue();
            if (qty != null && qty > 0 && itemsCategory.get(itemId) == itemCategory) {
                // determine full price of that item (requires external price lookup)
                if(!isDouble){
                    double itemPrice = prices.get(itemId).get();
                    double discountedPrice = itemPrice * (100 - percentage) / 100;
                    itemsDiscountedPrices.put(itemId,Math.min(itemsDiscountedPrices.get(itemId), discountedPrice));
                }else{
                    double itemPrice = itemsDiscountedPrices.get(itemId);
                    double discountedPrice = itemPrice * (100 - percentage) / 100;
                    itemsDiscountedPrices.put(itemId, discountedPrice);
                }
            }
        }
        return itemsDiscountedPrices;
    }

    @Override
    public boolean checkPolicies(Map<Integer,Integer> items, Map<Integer,Double> prices, Map<Integer,ItemCategory> itemsCategory) {
        if(policyHead == null) {
            return true;
        }
        return policyHead.test(items, prices, itemsCategory);
    }

    @Override
    public boolean isDouble() {
        return isDouble;
    }

    private void validatePercentage(int p) {
        if (p < 0 || p > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
    }

    public ItemCategory getCategory() {
        return itemCategory;
    }

}
