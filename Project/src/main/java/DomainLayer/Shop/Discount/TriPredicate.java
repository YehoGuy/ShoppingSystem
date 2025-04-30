package DomainLayer.Shop.Discount;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import DomainLayer.Item.ItemCategory;

/**
 * Functional interface for testing a cart against three inputs:
 *  items→qty, prices, and categories.
 */
@FunctionalInterface
public interface TriPredicate<A,B,C> {
    boolean test(A a, B b, C c);
}

