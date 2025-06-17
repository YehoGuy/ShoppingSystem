package com.example.app.DBLayer.Item;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.Item.IItemRepository;
import com.example.app.DomainLayer.Item.Item;

@Profile("!no-db & !test")
public interface ItemRepositoryDB extends JpaRepository<Item, Integer>, IItemRepository {
    // You can add custom query methods here if needed
}
