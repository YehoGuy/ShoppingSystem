package com.example.app.DomainLayer;

import jakarta.persistence.Embeddable;
import java.util.concurrent.ConcurrentHashMap;

@Embeddable
public class ItemMapping{
        final private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer,Integer>> items; // shopID, (productID, quantity)  
    public ItemMapping() {
        this.items = new ConcurrentHashMap<>();
    }

    public void putIfAbsent(int shopId, ConcurrentHashMap<Integer, Integer> basket) {
        items.putIfAbsent(shopId, basket);
    }

    public ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> getItems() {
        return items;
    }

    public ConcurrentHashMap<Integer, Integer> getOrDefault(int shopId, ConcurrentHashMap<Integer, Integer> defaultValue) {
        return items.getOrDefault(shopId, defaultValue);
    }

    public void remove(int shopId) {
        items.remove(shopId);
    }

    public void clear() {
        items.clear();
    }

    public void put(int shopId, ConcurrentHashMap<Integer, Integer> basket) {
        items.put(shopId, basket);
    }

    public boolean containsKey(int shopId) {
        return items.containsKey(shopId);
    }

    public ConcurrentHashMap<Integer, Integer> get(int shopId) {
        return items.get(shopId);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void removeItem(int shopId, int productId) {
        ConcurrentHashMap<Integer, Integer> shopItems = items.get(shopId);
        if (shopItems != null) {
            shopItems.remove(productId);
            if (shopItems.isEmpty()) {
                items.remove(shopId); // Remove the shop if it has no items left
            }
        }
    }
}
