package com.example.app.DomainLayer.Purchase;

import java.time.LocalDateTime;
import java.util.Map;
import jakarta.persistence.Embeddable;

@Embeddable
public class BidReciept extends Reciept{

    private final int thisBidderId;

    private final int initialPrice;
    private final int highestBid; // initialPrice if no Bidder
    private final int highestBidderId; // -1 if no Bidder
    private final LocalDateTime endTime; // End time of the auction, if applicable


    public BidReciept(int purchaseId, int userId, int storeId, Map<Integer, Integer> items, Address shippingAddress,int price, int thisBidderId, int initialPrice, int highestBid, int highestBidderId, boolean isCompleted, LocalDateTime endTime) {
        super(purchaseId, userId, storeId, items, shippingAddress, price, isCompleted);
        this.thisBidderId = thisBidderId;
        this.initialPrice = initialPrice;
        this.highestBid = highestBid;
        this.highestBidderId = highestBidderId;
        this.endTime = endTime;
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

    public LocalDateTime getEndTime() {
        return endTime;
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
                ", endTime=" + endTime +
                '}';
    }

}
