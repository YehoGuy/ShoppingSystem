package DTOs;

import java.util.List;

import Domain.ItemCategory;

public class ItemDTO {
    private int id;
    private String name;
    private String description;
    private String category;
    private double averageRating;
    private List<ItemReviewDTO> reviews;

    // Jackson needs this
    public ItemDTO() {
    }

    public ItemDTO(int id, String name, String description, String category, double averageRating,
            List<ItemReviewDTO> reviews) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.averageRating = averageRating;
        this.reviews = reviews;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public List<ItemReviewDTO> getReviews() {
        return reviews;
    }

    // Setters (required by Jackson)
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public void setReviews(List<ItemReviewDTO> reviews) {
        this.reviews = reviews;
    }
}
