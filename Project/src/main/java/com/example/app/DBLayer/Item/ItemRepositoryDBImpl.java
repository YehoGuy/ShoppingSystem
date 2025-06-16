package com.example.app.DBLayer.Item;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DomainLayer.Item.IItemRepository;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Item.ItemReview;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
@Profile("!no-db & !test")
public class ItemRepositoryDBImpl implements IItemRepository {

    // This class can implement methods from IItemRepository
    // and use ItemRepositoryDB for database operations.

    private final ItemRepositoryDB jpaRepo;

    @PersistenceContext
    private EntityManager entityManager;

    private final AtomicInteger itemIdCounter = new AtomicInteger(1);

    public ItemRepositoryDBImpl(@Lazy @Autowired ItemRepositoryDB jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Integer createItem(String name, String description, Integer category) {
        Item item = new Item(itemIdCounter.incrementAndGet(), name, description, category);
        Item saved = jpaRepo.save(item);
        if (saved == null) {
            throw new OurRuntime("Failed to save item: " + item);
        }
        return saved.getId();
    }

    @Override
    public Item getItem(int itemId) {
        return jpaRepo.findById(itemId)
                .orElseThrow(() -> new OurRuntime("Item not found: " + itemId));
    }

    @Override
    public List<Item> getAllItems() {
        return jpaRepo.findAll();
    }

    @Override
    public void addReviewToItem(int itemId, int rating, String reviewText) {
        Item item = getItem(itemId);
        item.addReview(rating, reviewText);
        jpaRepo.save(item);
    }

    @Override
    public List<ItemReview> getItemReviews(int itemId) {
        Item item = getItem(itemId);
        return item.getReviews();
    }

    @Override
    public double getItemAverageRating(int itemId) {
        List<ItemReview> reviews = getItemReviews(itemId);
        return reviews.stream()
                .mapToInt(ItemReview::getRating)
                .average()
                .orElse(0.0);
    }

    @Override
    public void deleteItem(int itemId) {
        jpaRepo.deleteById(itemId);
    }

    @Override
    public List<Item> getItemsByIds(List<Integer> itemIds) {
        return jpaRepo.findAllById(itemIds);
    }

    @Override
    public List<Integer> getItemsByCategory(ItemCategory category) {
        return getAllItems().stream()
                .filter(item -> item.getCategory() == category)
                .map(Item::getId)
                .toList();
    }

}
