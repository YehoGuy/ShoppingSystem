package DomainLayerTests;

import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.BidReciept;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.Map;

public class BidRecieptTests {

    @Test
    void fullConstructor_setsFieldsCorrectly() {
        Map<Integer,Integer> items = Map.of(1,1);
        Address addr = new Address();
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        BidReciept br = new BidReciept(5, 6, 7, items, addr, 150, 99, 100, 200, 42, true, end);

        assertAll(
            () -> assertEquals(5, br.getPurchaseId()),
            () -> assertEquals(6, br.getUserId()),
            () -> assertEquals(7, br.getShopId()),
            () -> assertEquals(150, br.getPrice()),
            () -> assertEquals(99, br.getThisBidderId()),
            () -> assertEquals(100, br.getInitialPrice()),
            () -> assertEquals(200, br.getHighestBid()),
            () -> assertEquals(42, br.getHighestBidderId()),
            () -> assertTrue(br.isCompleted()),
            () -> assertEquals(end, br.getEndTime())
        );
    }

    @Test
    void defaultConstructor_setsDefaults() {
        BidReciept br = new BidReciept();
        assertAll(
            () -> assertEquals(-1, br.getThisBidderId()),
            () -> assertEquals(0, br.getHighestBid()),
            () -> assertEquals(-1, br.getHighestBidderId()),
            () -> assertNotNull(br.toString())
        );
    }
}
