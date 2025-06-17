package DBLayerTests;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.app.SimpleHttpServerApplication;
import com.example.app.DBLayer.Purchase.PurchaseRepositoryDBImpl;
import com.example.app.DomainLayer.Purchase.Address;

import jakarta.transaction.Transactional;

@SpringBootTest(classes = SimpleHttpServerApplication.class)
@ActiveProfiles({ "test" })
@Transactional
public class PurchaseRepositoryDBImplTests {

    @Autowired
    private PurchaseRepositoryDBImpl repo;

    @BeforeEach
    void setup() {
    }

    @Test
    void testAddPurchase_Success() {
        int id = repo.addPurchase(0, 0, new HashMap<>(), 0, new Address());
        assert id == 1;
    }

}
