package DomainLayer.Shop.Discount;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CompositeDiscount implements Discount {
    public enum Operator { AND, OR, XOR }

    private final Operator operator;
    private final List<Discount> children = new ArrayList<>();

    public CompositeDiscount(Operator operator) {
        this.operator = operator;
    }

    public void add(Discount d){ 
        children.add(d); 
    }
    public void remove(Discount d){ 
        children.remove(d); 
    }

    @Override
    public Integer getItemId() {
        return null;
    }

    @Override
    public int getPercentage() {
        throw new UnsupportedOperationException("CompositeDiscount אין לו אחוז יחיד");
    }

    @Override
    public void setPercentage(int percentage) {
        throw new UnsupportedOperationException("אין תמיכה ב־setPercentage עבור קומפוזיט");
    }

    @Override
    public Map<Integer,Integer> applyDiscounts(Map<Integer,Integer> items, Map<Integer,AtomicInteger> prices, Map<Integer,Integer> itemsDiscountedPrices) {
        List<Map<Integer,Integer>> results = new ArrayList<>();
        for (Discount d : children) {
            Map<Integer,Integer> copy = new HashMap<>(itemsDiscountedPrices);
            results.add(d.applyDiscounts(items, prices, copy));
        }

        switch (operator) {
            case AND:
                return results.stream()
                    .reduce((m1, m2) -> {
                        Map<Integer,Integer> merged = new HashMap<>();
                        for (Integer id : m1.keySet()) {
                            // קיבלנו לכל פריט את המחיר המינימלי מבין שתי המפות
                            merged.put(id, Math.min(m1.get(id), m2.get(id)));
                        }
                        return merged;
                    })
                    .orElse(itemsDiscountedPrices);

            case OR:
                // apply only *הנחה אחת*: בוחרים את המפה שמנמיכה הכי הרבה
                return results.stream()
                    .max(Comparator.comparingDouble(m -> total(m, items)))
                    .orElse(itemsDiscountedPrices);

            case XOR:
                // בדיוק *אחת* מהילדים צריכה לחול: אם 0 או >1, מחזירים ללא שינוי
                int countApplied = 0;
                Map<Integer,Integer> last = null;
                for (Map<Integer,Integer> r : results) {
                    if (!r.equals(itemsDiscountedPrices)) {
                        countApplied++;
                        last = r;
                    }
                }
                return (countApplied == 1) ? last : itemsDiscountedPrices;

            default:
                return itemsDiscountedPrices;
        }
    }

    // עזר לחישוב סכום כולל מתוך מפה של discountedPrices
    private double total(Map<Integer,Integer> discounted, Map<Integer,Integer> items) {
        double sum = 0;
        for (Map.Entry<Integer,Integer> e : items.entrySet()) {
            int id = e.getKey(), qty = e.getValue();
            sum += discounted.get(id) * qty;
        }
        return sum;
    }
}
