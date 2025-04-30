package DomainLayer.Shop.Discount;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import DomainLayer.Item.ItemCategory;

public class CompositeDiscount implements Discount {
    public enum Operator { AND, OR, XOR }

    private final Operator operator;
    private final boolean doubleDiscount;
    private final List<Policy> policies;
    private final List<Discount> children;

    /**
     * @param operator       logical operator to combine policies
     * @param doubleDiscount if true, child discounts stack sequentially; if false, uses the most generous (lowest price)
     * @param policies       list of conditions that must be satisfied
     * @param discounts      list of child discounts to apply if policies pass
     */
    public CompositeDiscount(Operator operator,
                             boolean doubleDiscount,
                             List<Policy> policies,
                             List<Discount> discounts) {
        this.operator = operator;
        this.doubleDiscount = doubleDiscount;
        this.policies = new ArrayList<>(policies);
        this.children = discounts == null ? new ArrayList<>() : new ArrayList<>(discounts);
    }

    public void add(Discount d) {
        children.add(d);
    }

    public void remove(Discount d) {
        children.remove(d);
    }

    /**
     * Applies child discounts only if combined policy predicate holds.
     * If doubleDiscount is true, applies each child in sequence to the current best price;
     * otherwise selects the single best discount (lowest resulting price).
     */
    @Override
    public Map<Integer, Integer> applyDiscounts(
            Map<Integer, Integer> items,
            Map<Integer, Integer> prices,
            Map<Integer, ItemCategory> itemsCategory) {
        // Evaluate policy combination
        boolean condition;
        if(policies.isEmpty()) {
            // No policies: always true
            condition = true;
        } else if (policies.size() == 1) {
            // Single policy: just evaluate it
            condition = policies.get(0).test(items, prices, itemsCategory);
        } else {
            switch (operator) {
                case AND:
                    condition = policies.stream()
                        .allMatch(p -> p.test(items, prices, itemsCategory));
                    break;
                case OR:
                    condition = policies.stream()
                        .anyMatch(p -> p.test(items, prices, itemsCategory));
                    break;
                case XOR:
                    long trueCount = policies.stream()
                        .filter(p -> p.test(items, prices, itemsCategory))
                        .count();
                    condition = (trueCount == 1);
                    break;
                default:
                    condition = false; // should never happen
            }
        }
        Map<Integer, Integer> currentDiscounts;

        if (!condition) {
            currentDiscounts = new HashMap<>();
            for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
                int id = entry.getKey();
                currentDiscounts.put(id, prices.get(id));
            }
            return currentDiscounts;
        }

        // Initialize working map: full prices or empty
        if (doubleDiscount) {
            currentDiscounts = new HashMap<>();
            for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
                int id = entry.getKey();
                currentDiscounts.put(id, prices.get(id));
            }
            // Sequentially apply each child discount
            for (Discount child : children) {
                currentDiscounts = child.applyDiscounts(items, currentDiscounts, itemsCategory);
            }
        } else {
            // Compute each child's result, take the lowest price per item
            Map<Integer, Integer> best = new HashMap<>();
            for (Discount child : children) {
                Map<Integer, Integer> result = child.applyDiscounts(items, prices, itemsCategory);
                for (Map.Entry<Integer, Integer> e : result.entrySet()) {
                    best.merge(e.getKey(), e.getValue(), Math::min);
                }
            }
            currentDiscounts = best;
        }
        return currentDiscounts;
    }
}
