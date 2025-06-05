package com.example.app.DomainLayer.Item;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemCategory category;
 
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemReview> reviews = new ArrayList<>();

    public Item() { }

    public Item(Integer id, String name, String description, ItemCategory category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.reviews = new ArrayList<>();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Getters & setters for all persisted fields:
    // ──────────────────────────────────────────────────────────────────────────────
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public void setCategory(ItemCategory category) {
        this.category = category;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // In‐memory reviews (JPA ignores them):
    // ──────────────────────────────────────────────────────────────────────────────
    public List<ItemReview> getReviews() {
        return Collections.unmodifiableList(reviews);
    }

    /**
     * Adds a new review to the item.
     *
     * @param rating     the review rating.
     * @param reviewText the review text.
     */
    public void addReview(int rating, String reviewText) {
        addReview(new ItemReview(rating, reviewText));
    }

    /**
     * Adds an ItemReview to the item.
     *
     * @param review the review to add.
     */
    public void addReview(ItemReview review) {
        synchronized (reviews) {
            reviews.add(review);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Other business logic unchanged:
    // ──────────────────────────────────────────────────────────────────────────────
    public double getAverageRating() {
        if (reviews.isEmpty()) return 0.0;
        int sum = 0;
        for (ItemReview r : reviews) sum += r.getRating();
        return (double) sum / reviews.size();
    }

    @Override
    public String toString() {
        return "Item{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", category=" + category +
               ", avgRating=" + getAverageRating() +
               '}';
    }
}
