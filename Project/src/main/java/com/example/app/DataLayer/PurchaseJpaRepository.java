package com.example.app.DataLayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.DomainLayer.Purchase.Purchase;

@Repository
public interface PurchaseJpaRepository extends JpaRepository<Purchase, Integer>{

}
