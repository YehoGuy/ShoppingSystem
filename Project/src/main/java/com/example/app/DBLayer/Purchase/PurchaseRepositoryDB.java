package com.example.app.DBLayer.Purchase;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.DomainLayer.Purchase.IPurchaseRepository;
import com.example.app.DomainLayer.Purchase.Purchase;

public interface PurchaseRepositoryDB extends JpaRepository<Purchase, Integer>, IPurchaseRepository {

}
