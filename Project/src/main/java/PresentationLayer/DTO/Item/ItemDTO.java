package PresentationLayer.DTO.Item;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ItemDTO(
        @Positive int id,
        @NotBlank String name,
        String description,
        String category,                   // enum name as String
        double averageRating,
        List<ItemReviewDTO> reviews) {

    /* Domain → DTO /
    public static ItemDTO fromDomain(DomainLayer.Item.Item item) {
        return new ItemDTO(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getCategory().name(),
                item.getAverageRating(),
                item.getReviews()
                    .stream()
                    .map(ItemReviewDTO::fromDomain)
                    .toList()
        );
    }

    / DTO → Domain (useful for create/update) */
    public DomainLayer.Item.Item toDomain() {
        DomainLayer.Item.Item i = new DomainLayer.Item.Item(
                id, name, description,
                DomainLayer.Item.ItemCategory.valueOf(category).ordinal()
        );
        reviews.forEach(r -> i.addReview(r.toDomain()));
        return i;
    }
}