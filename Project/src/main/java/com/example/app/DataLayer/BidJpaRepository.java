package com.example.app.DataLayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.DomainLayer.Purchase.Bid;

@Repository
public interface BidJpaRepository extends JpaRepository<Bid, Integer> {
    

}
