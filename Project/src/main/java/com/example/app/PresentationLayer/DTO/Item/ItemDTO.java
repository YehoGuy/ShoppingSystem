package com.example.app.PresentationLayer.DTO.Item;
import java.util.List;

public record ItemDTO(
    int id,
    String name,
    String description,
    String category,
    double averageRating,
    List<ItemReviewDTO> reviews
) {
    /* -------- Domain → DTO -------- */
    public static ItemDTO fromDomain(com.example.app.DomainLayer.Item.Item item) {
        return new ItemDTO(
            item.getId(),
            item.getName(),
            item.getDescription(),
            item.getCategory().toString(),
            item.getAverageRating(),
            item.getReviews().stream().map(ItemReviewDTO::fromDomain).toList()
        );
    }
}