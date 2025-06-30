package DBLayerTests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.app.SimpleHttpServerApplication;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DBLayer.Purchase.PurchaseRepositoryDBImpl;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Purchase;

import jakarta.transaction.Transactional;

@SpringBootTest(classes = SimpleHttpServerApplication.class)
@ActiveProfiles("db-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
public class PurchaseRepositoryDBImplTests {

    @Autowired
    private PurchaseRepositoryDBImpl repo;
    int id;

    @BeforeEach
    void setup() {
        id = repo.addPurchase(0, 0, new HashMap<>(), 0, new Address());
    }

    @Test
    void testAddPurchase_Success() {
        Address shipping = new Address();
        int id = repo.addPurchase(1, 1, new HashMap<>(), 0, shipping);
        assertDoesNotThrow(() -> repo.getPurchaseById(id));
    }

    @Test
    void testAddBid_simpleFunction_Success() {
        int id2 = repo.addBid(0, 0, new HashMap<>(), 0);
        assertDoesNotThrow(() -> repo.getPurchaseById(id2));
    }

    @Test
    void testAddBid_complexFunction_Success() {
        int id2 = repo.addBid(0, 0, new HashMap<>(), 0, null, null);
        assertDoesNotThrow(() -> repo.getPurchaseById(id2));
    }

    @Test
    void testGetPurchaseById_Success() {
        int id = repo.addBid(1, 1, new HashMap<>(), 100, null, null);
        int id2 = repo.addBid(1, 1, new HashMap<>(), 200, null, null);
        assertDoesNotThrow(() -> repo.getPurchaseById(id));
        assertDoesNotThrow(() -> repo.getPurchaseById(id2));
    }

    @Test
    void testGetPurchaseById_InvalidId_Failure() {
        assertThrows(OurRuntime.class, () -> repo.getPurchaseById(37));
    }

    @Test
    void testDeletePurchase_Success() {
        int userId = 0;
        int storeId = 0;
        HashMap<Integer, Integer> products = new HashMap<>();
        double price = 0;
        Address address = new Address();
        int id = repo.addPurchase(userId, storeId, products, price, address);

        repo.deletePurchase(id);
        assertThrows(OurRuntime.class, () -> repo.getPurchaseById(id));
    }

    @Test
    void testGetUserPurchases_Success() {
        assert repo.getUserPurchases(0).size() == 1;
    }

    @Test
    void testGetUserPurchases_EmptyList() {
        assert repo.getUserPurchases(37).size() == 0;
    }

    @Test
    void testGetStorePurchases_Success() {
        assert repo.getStorePurchases(0).size() == 1;
    }

    @Test
    void testGetStorePurchases_EmptyList() {
        assert repo.getStorePurchases(37).size() == 0;
    }

    @Test
    void testGetUserStorePurchases_Success() {
        assert repo.getUserStorePurchases(0, 0).size() == 1;
    }

    @Test
    void testGetAllBids_Success() {
        repo.addBid(0, 0, new HashMap<>(), 0, null, null);
        assert repo.getAllBids().size() == 1;
    }

    @Test
    void testGetAllBids_EmptyList() {
        assert repo.getAllBids().size() == 0;
    }

    @Test
    void testGetShopBids_Success() {
        repo.addBid(0, 0, new HashMap<>(), 0, null, null);
        assert repo.getShopBids(0).size() == 1;
    }

}
