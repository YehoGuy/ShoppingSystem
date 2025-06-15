package com.example.app.DomainLayer.Shop.Discount;

import jakarta.persistence.Embeddable;

/**
 * Functional interface for testing a cart against three inputs:
 * items→qty, prices, and categories.
 */
@Embeddable
@FunctionalInterface
public interface TriPredicate<A, B, C> {
    boolean test(A a, B b, C c);
}
