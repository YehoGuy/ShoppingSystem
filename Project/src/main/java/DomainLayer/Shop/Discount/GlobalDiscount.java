package DomainLayer.Shop.Discount;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import DomainLayer.Item.ItemCategory;

public class GlobalDiscount implements Discount {
    
    private final int percentage;
    private Policy policyHead;
    private boolean isDouble;

    public GlobalDiscount(int percentage, Policy policyHead, boolean isDouble) {
        validatePercentage(percentage);
        this.percentage = percentage;
        this.policyHead = policyHead;
        this.isDouble = isDouble;
    }


    @Override
    public Map<Integer, Integer> applyDiscounts(Map<Integer, Integer> items, Map<Integer, AtomicInteger> prices, Map<Integer, Integer> itemsDiscountedPrices, Map<Integer, ItemCategory> itemsCategory) {
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            Integer itemId = entry.getKey();
            Integer qty = entry.getValue();
            if (qty != null && qty > 0) {
                // determine full price of that item (requires external price lookup)
                if(!isDouble){
                    int itemPrice = prices.get(itemId).get();
                    int discountedPrice = itemPrice * (100 - percentage) / 100;
                    itemsDiscountedPrices.put(itemId,Math.min(itemsDiscountedPrices.get(itemId), discountedPrice));
                }else{
                    int itemPrice = itemsDiscountedPrices.get(itemId);
                    int discountedPrice = itemPrice * (100 - percentage) / 100;
                    itemsDiscountedPrices.put(itemId, discountedPrice);
                }
            }
        }
        return itemsDiscountedPrices;
    }

    @Override
    public boolean checkPolicies(Map<Integer,Integer> items, Map<Integer,Integer> prices, Map<Integer,ItemCategory> itemsCategory) {
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
}

