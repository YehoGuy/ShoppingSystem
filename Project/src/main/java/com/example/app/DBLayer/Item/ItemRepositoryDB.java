package com.example.app.DBLayer.Item;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.Item.Item;

public interface ItemRepositoryDB extends JpaRepository<Item, Integer> {
    // You can add custom query methods here if needed
}
