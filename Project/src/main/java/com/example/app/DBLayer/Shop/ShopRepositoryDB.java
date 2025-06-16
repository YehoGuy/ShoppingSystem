package com.example.app.DBLayer.Shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.DomainLayer.Shop.IShopRepository;
import com.example.app.DomainLayer.Shop.Shop;

@Repository
public interface ShopRepositoryDB extends JpaRepository<Shop, Integer>, IShopRepository {
    // This interface extends JpaRepository to provide CRUD operations for Shop
    // entities.
    // Additional custom query methods can be defined here if needed.
}
