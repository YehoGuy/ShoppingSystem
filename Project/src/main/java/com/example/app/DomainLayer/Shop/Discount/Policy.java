package com.example.app.DomainLayer.Shop.Discount;

import java.util.Map;

import com.example.app.DomainLayer.Item.ItemCategory;

public interface Policy {

    boolean test(Map<Integer,Integer> items, Map<Integer,Double> prices, Map<Integer,ItemCategory> itemsCategory);
}
