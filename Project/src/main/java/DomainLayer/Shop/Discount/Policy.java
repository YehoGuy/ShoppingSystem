package DomainLayer.Shop.Discount;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import DomainLayer.Item.ItemCategory;
import java.util.Objects;

/**
 * A conditional policy that inspects the cart (items, prices, categories)
 * and returns true/false.
 */
public class Policy {
    private final TriPredicate<Map<Integer,Integer>,Map<Integer,Integer>,Map<Integer,ItemCategory>> predicate;

    /**
     * @param predicate a lambda taking (items→qty, prices, categories) and returning boolean
     */
    public Policy(TriPredicate<Map<Integer,Integer>, Map<Integer,Integer>,Map<Integer,ItemCategory>> predicate) {
        this.predicate = Objects.requireNonNull(predicate, "predicate");
    }

    /**
     * Apply the policy to the given cart.
     *
     * @param items         map of itemId→quantity
     * @param prices        map of itemId→price‐AtomicInteger
     * @param itemsCategory map of itemId→its category
     * @return true if the predicate passes, false otherwise
     */
    public boolean test(Map<Integer,Integer> items, Map<Integer,Integer> prices, Map<Integer,ItemCategory> itemsCategory) {
        return predicate.test(items, prices, itemsCategory);
    }
}
