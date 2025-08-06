package DomainLayerTests;

import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Reciept;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.Map;

public class RecieptTests {

    @Test
    void constructorIncomplete_setsFieldsAndTimestamp() {
        Map<Integer,Integer> items = Map.of(1,1, 2,2);
        Address addr = new Address();
        Reciept r = new Reciept(10, 20, 30, items, addr, 99.99, false);

        assertAll(
            () -> assertEquals(10, r.getPurchaseId()),
            () -> assertEquals(20, r.getUserId()),
            () -> assertEquals(30, r.getShopId()),
            () -> assertEquals(items, r.getItems()),
            () -> assertEquals(addr.toString(), r.getShippingAddressString()),
            () -> assertEquals(99.99, r.getPrice()),
            () -> assertFalse(r.isCompleted()),
            () -> assertNull(r.getTimeOfCompletion()),
            () -> assertNull(r.getEndTime()),
            () -> assertNotNull(r.getTimestampOfRecieptGeneration())
        );
    }

    @Test
    void constructorComplete_setsCompletedAndTime() {
        Map<Integer,Integer> items = Map.of(5,3);
        Address addr = new Address();
        LocalDateTime time = LocalDateTime.now().minusDays(1);
        Reciept r = new Reciept(1,2,3, items, addr, time, 50.0);

        assertAll(
            () -> assertTrue(r.isCompleted()),
            () -> assertEquals(time, r.getTimeOfCompletion()),
            () -> assertEquals(50.0, r.getPrice()),
            () -> assertNull(r.getEndTime()),
            () -> assertNotNull(r.getTimestampOfRecieptGeneration())
        );
    }

    @Test
    void constructorAuction_setsEndTime() {
        Map<Integer,Integer> items = Map.of();
        Address addr = new Address();
        LocalDateTime time = LocalDateTime.now();
        LocalDateTime end = time.plusHours(2);
        Reciept r = new Reciept(0,0,0, items, addr, time, 0.0, end);

        assertAll(
            () -> assertTrue(r.isCompleted()),
            () -> assertEquals(end, r.getEndTime())
        );
    }

    @Test
    void defaultConstructor_worksAndGettersReturnDefaults() {
        Reciept r = new Reciept();
        assertAll(
            () -> assertEquals(-1, r.getPurchaseId()),
            () -> assertFalse(r.isCompleted()),
            () -> assertEquals(0.0, r.getPrice())
        );
    }

    @Test
    void toString_containsKeyFields() {
        Reciept r = new Reciept();
        String s = r.toString();
        assertTrue(s.contains("purchaseId=-1"));
        assertTrue(s.contains("isCompleted="));
    }
}
