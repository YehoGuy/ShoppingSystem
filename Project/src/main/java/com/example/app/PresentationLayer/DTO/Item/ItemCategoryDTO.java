package com.example.app.PresentationLayer.DTO.Item;

public record ItemCategoryDTO(String name) {

    public static ItemCategoryDTO fromDomain(com.example.app.DomainLayer.Item.ItemCategory c) {
        return new ItemCategoryDTO(c.name());
    }
}