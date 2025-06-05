package com.example.app.DataLayer;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.Item.Item;

public interface ItemJpaRepository extends JpaRepository<Item, Long> {
    // Additional query methods can be defined here if needed
}
