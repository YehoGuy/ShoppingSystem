package com.example.app.DBLayer.Item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.DomainLayer.Item.IItemRepository;
import com.example.app.DomainLayer.Item.Item;

@Repository
public interface ItemRepositoryDB extends JpaRepository<Item, Integer>, IItemRepository {
    // You can add custom query methods here if needed
}
