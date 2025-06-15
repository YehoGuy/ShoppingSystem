package com.example.app.DomainLayer.Shop.Discount;

import java.util.Map;

import com.example.app.DomainLayer.Item.ItemCategory;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "policy_type")
public abstract class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    public abstract boolean test(Map<Integer, Integer> items, Map<Integer, Double> prices,
            Map<Integer, ItemCategory> itemsCategory);
}
