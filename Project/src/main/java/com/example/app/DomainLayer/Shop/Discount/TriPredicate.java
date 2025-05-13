package com.example.app.DomainLayer.Shop.Discount;


/**
 * Functional interface for testing a cart against three inputs:
 *  items→qty, prices, and categories.
 */
@FunctionalInterface
public interface TriPredicate<A,B,C> {
    boolean test(A a, B b, C c);
}

