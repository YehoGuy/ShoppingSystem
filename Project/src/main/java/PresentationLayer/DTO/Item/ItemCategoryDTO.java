package PresentationLayer.DTO.Item;

public record ItemCategoryDTO(String name) {

    public static ItemCategoryDTO fromDomain(DomainLayer.Item.ItemCategory c) {
        return new ItemCategoryDTO(c.name());
    }
}