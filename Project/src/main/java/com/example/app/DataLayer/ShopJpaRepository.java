package com.example.app.DataLayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.DomainLayer.Shop.Shop;

@Repository
public interface ShopJpaRepository extends JpaRepository<Shop, Integer> {
    // no additional methods needed; Spring Data will provide save/find/delete, etc.
}