package com.example.app.DBLayer.Purchase;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.DomainLayer.Purchase.IPurchaseRepository;
import com.example.app.DomainLayer.Purchase.Purchase;
import com.example.app.DomainLayer.Purchase.Reciept;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Profile("!no-db & !test")
@Repository
public class PurchaseRepositoryDBImpl implements IPurchaseRepository {

    private AtomicInteger purchaseIdCounter = new AtomicInteger(1);

    private PurchaseRepositoryDB jpaRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public PurchaseRepositoryDBImpl(@Lazy @Autowired PurchaseRepositoryDB jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public int addPurchase(int userId, int storeId, Map<Integer, Integer> items, double price,
            Address shippingAddresse) {
        int id = purchaseIdCounter.getAndIncrement();
        Purchase purchase = new Purchase(id, userId, storeId, shippingAddresse);
        try {
            jpaRepo.save(purchase);
            return id;
        } catch (Exception e) {
            throw new OurRuntime("Error when creating purchase.");
        }
    }

    @Override
    public int addBid(int userId, int storeId, Map<Integer, Integer> items, int initialPrice) {
        int id = purchaseIdCounter.getAndIncrement();
        Bid bid = new Bid(id, userId, storeId, items, initialPrice);
        try {
            jpaRepo.save(bid);
            return id;
        } catch (Exception e) {
            throw new OurRuntime("Error when creating bid");
        }
    }

    @Override
    public int addBid(int userId, int storeId, Map<Integer, Integer> items, int initialPrice,
            LocalDateTime auctionStart, LocalDateTime auctionEnd) {
        int id = purchaseIdCounter.getAndIncrement();
        Bid bid = new Bid(id, userId, storeId, items, initialPrice, auctionStart, auctionEnd);
        try {
            jpaRepo.save(bid);
            return id;
        } catch (Exception e) {
            throw new OurRuntime("Error when creating auction");
        }
    }

    @Override
    public Purchase getPurchaseById(int purchaseId) {
        return jpaRepo.findById(purchaseId).orElseThrow(() -> new OurRuntime("Error when trying to fetch purchase."));
    }

    @Override
    public void deletePurchase(int purchaseId) {
        try {
            jpaRepo.delete(getPurchaseById(purchaseId));
        } catch (Exception e) {
            throw new OurRuntime(e.getCause());
        }
    }

    @Override
    public List<Reciept> getUserPurchases(int userId) {
        return jpaRepo.findAll().stream().filter(purchase -> purchase.getUserId() == userId)
                .map(Purchase::generateReciept).toList();
    }

    @Override
    public List<Reciept> getStorePurchases(int storeId) {
        return jpaRepo.findAll().stream().filter(purchase -> purchase.getStoreId() == storeId)
                .map(Purchase::generateReciept).toList();
    }

    @Override
    public List<Reciept> getUserStorePurchases(int userId, int storeId) {
        return jpaRepo.findAll().stream().filter(purchase -> purchase.getUserId() == userId)
                .filter(purchase -> purchase.getStoreId() == storeId)
                .map(Purchase::generateReciept).toList();
    }

    @Override
    public List<BidReciept> getAllBids() {
        List<Purchase> bids = jpaRepo.findAll().stream().filter(purchase -> purchase instanceof Bid).toList();
        List<BidReciept> reciepts = new LinkedList<>();
        for (Purchase bid : bids) {
            if (bid instanceof Bid)
                reciepts.add(((Bid) bid).generateReciept());
        }
        return reciepts;
    }

    @Override
    public List<BidReciept> getShopBids(int shopId) {
        List<Purchase> bids = jpaRepo.findAll().stream().filter(purchase -> purchase instanceof Bid)
                .filter(purchase -> purchase.getStoreId() == shopId).toList();
        List<BidReciept> reciepts = new LinkedList<>();
        for (Purchase bid : bids) {
            if (bid instanceof Bid)
                reciepts.add(((Bid) bid).generateReciept());
        }
        return reciepts;
    }

}
