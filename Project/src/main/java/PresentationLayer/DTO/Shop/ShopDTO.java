package PresentationLayer.DTO.Shop;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 
A read/write DTO for shop endpoints.
For “list shops” you might return a slimmer “ShopSummaryDTO”, but
this works fine for create / detail views.*/
public record ShopDTO(
        @Positive int id,
        @NotBlank String name,
        double averageRating,
        List<ShopReviewDTO> reviews) {

    /* ------------- Domain ➜ DTO (for GET endpoints) ------------- */
    public static ShopDTO fromDomain(DomainLayer.Shop.Shop s) {
        return new ShopDTO(
                s.getId(),
                s.getName(),
                s.getAverageRating(),
                s.getReviews()
                 .stream()
                 .map(ShopReviewDTO::fromDomain)
                 .toList());
    }


}