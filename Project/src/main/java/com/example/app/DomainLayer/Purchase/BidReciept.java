package com.example.app.DomainLayer.Purchase;

import java.time.LocalDateTime;
import java.util.Map;

public class BidReciept extends Reciept{

    private final int thisBidderId;

    private final int initialPrice;
    private final int highestBid; // initialPrice if no Bidder
    private final int highestBidderId; // -1 if no Bidder

    public BidReciept(int purchaseId, int userId, int storeId, Map<Integer, Integer> items, Address shippingAddress,int price, int thisBidderId, int initialPrice, int highestBid, int highestBidderId) {
        super(purchaseId, userId, storeId, items, shippingAddress, price);
        this.thisBidderId = thisBidderId;
        this.initialPrice = initialPrice;
        this.highestBid = highestBid;
        this.highestBidderId = highestBidderId;
    }

    public int getThisBidderId() {
        return thisBidderId;
    }
    public int getInitialPrice() {
        return initialPrice;
    }
    public int getHighestBid() {
        return highestBid;
    }
    public int getHighestBidderId() {
        return highestBidderId;
    }


    @Override
    public String toString() {
        return "BidReciept{" +
                "thisBidderId=" + thisBidderId +
                ", initialPrice=" + initialPrice +
                ", highestBid=" + highestBid +
                ", highestBidderId=" + highestBidderId +
                ", yourBidderId=" + thisBidderId +
                ", purchaseId=" + purchaseId +
                ", userId=" + userId +
                ", storeId=" + storeId +
                ", items=" + items +
                ", shippingAddress=" + shippingAddress +
                ", isCompleted=" + isCompleted +
                ", timeOfCompletion=" + timeOfCompletion +
                ", price=" + price +
                ", timestampOfRecieptGeneration=" + LocalDateTime.now().toString() +
                '}';
    }

}
