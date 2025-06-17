package com.example.app.DBLayer.Shop;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.Shop.Shop;

public interface ShopRepositoryDB extends JpaRepository<Shop, Integer> {
    // This interface extends JpaRepository to provide CRUD operations for Shop
    // entities.
    // Additional custom query methods can be defined here if needed.
}
