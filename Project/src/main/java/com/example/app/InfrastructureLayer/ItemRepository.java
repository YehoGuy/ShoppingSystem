package com.example.app.InfrastructureLayer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.example.app.DomainLayer.Item.IItemRepository;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Item.ItemReview;

@Repository
public class ItemRepository implements IItemRepository {

    // Thread-safe map to store items by ID
    private final ConcurrentHashMap<Integer, Item> items = new ConcurrentHashMap<>();

    // Atomic counter to allocate unique item IDs
    private final AtomicInteger itemIdCounter = new AtomicInteger(1);

    @Override
    public Integer createItem(String name, String description, Integer category) {
        int id = itemIdCounter.getAndIncrement();
        Item item = new Item(id, name, description, category);
        Item previous = items.putIfAbsent(id, item);
        if (previous != null) {
            // Should never happen unless IDs clash
            throw new IllegalStateException("Item with id " + id + " already exists.");
        }
        return item.getId();
    }

    @Override
    public Item getItem(int itemId) {
        return items.get(itemId);
    }

    @Override
    public List<Item> getAllItems() {
        return Collections.unmodifiableList(
                items.values()
                        .stream()
                        .collect(Collectors.toList())
        );
    }

    @Override
    public void addReviewToItem(int itemId, int rating, String reviewText) {
        Item item = items.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }
        item.addReview(rating, reviewText);
    }

    @Override
    public List<ItemReview> getItemReviews(int itemId) {
        Item item = items.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }
        return item.getReviews();
    }

    @Override
    public double getItemAverageRating(int itemId) {
        Item item = items.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }
        return item.getAverageRating();
    }

    @Override
    public void deleteItem(int itemId) {
        Item removed = items.remove(itemId);
        if (removed == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }
    }

    @Override
    public List<Item> getItemsByIds(List<Integer> itemIds) {
        if (itemIds == null) {
            return Collections.emptyList();
        }
        List<Item> result = itemIds.stream()
                .map(items::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<Integer> getItemsByCategory(ItemCategory category) {
        if (category == null) {
            return Collections.emptyList();
        }
        List<Integer> result = items.values().stream()
                .filter(item -> item.getCategory() == category)
                .map(Item::getId)
                .collect(Collectors.toList());
        return Collections.unmodifiableList(result);
    }
}