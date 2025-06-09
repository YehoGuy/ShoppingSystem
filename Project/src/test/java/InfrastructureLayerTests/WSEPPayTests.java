package InfrastructureLayerTests;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.DomainLayer.Guest;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.User;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.InfrastructureLayer.UserRepository;
import com.example.app.DomainLayer.Notification;
import com.example.app.DomainLayer.ShoppingCart;

import com.example.app.InfrastructureLayer.WSEPPay;
import java.io.IOException;

public class WSEPPayTests {

    private WSEPPay wsepPay;

    @BeforeEach
    public void setUp() {
        wsepPay = new WSEPPay();
    }

    @Test
    public void testIsPaymentServiceAvailable() {
        try {
            boolean isAvailable = wsepPay.isPaymentServiceAvailable();
            assertTrue(isAvailable, "Payment service should be available");
        } catch (RuntimeException e) {
            assertFalse(true, "Payment service is not available: " + e.getMessage());
        }
    }

    @Test
    public void testProcessPaymentSuccess() throws Exception {
        
        double amount = 1000.5;
        String currency = "USD";
        String cardNumber = "2222333344445555"; // Valid Visa card number
        String expirationDateMonth = "4";
        String expirationDateYear = "2021";
        String cardHolderName = "Israel Israelovice";
        String cvv = "262";
        String id = "20444444";

        int result = wsepPay.processPayment(amount, currency, cardNumber, expirationDateMonth, expirationDateYear, cardHolderName, cvv, id);
        assertTrue(result > 10000 && result < 100000, "Payment ID should be between 10000 and 100000 and it is: " + result);
        assertNotNull(result, "Payment ID should not be null");
    }

    @Test
    public void testCancelPaymentSuccess() {
        double amount = 100.0; 
        String currency = "USD";
        String cardNumber = "4111111111111111"; // Valid Visa card number
        String expirationDateMonth = "12";
        String expirationDateYear = "2025";
        String cardHolderName = "John Doe";
        String cvv = "123";
        String id = "test-id";

        int result = wsepPay.processPayment(amount, currency, cardNumber, expirationDateMonth, expirationDateYear, cardHolderName, cvv, id);
        assertTrue(result > 10000 && result < 100000);
        boolean cancelResult = wsepPay.cancelPayment(result);
        assertTrue(cancelResult, "Payment cancellation should be successful");
    }

    @Test
    void testProcessPaymentWithNullCurrency() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepPay.processPayment(100.0, null, "4111111111111111", "12", "2025", "John Doe", "123", "test-id");
        });
    }

    @Test 
    void testProcessPaymentWithNullCardNumber() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepPay.processPayment(100.0, "USD", null, "12", "2025", "John Doe", "123", "test-id");
        });
    }

    @Test
    void testProcessPaymentWithNullCardholderName() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepPay.processPayment(100.0, "USD", "4111111111111111", "12", "2025", null, "123", "test-id"); 
        });
    }

    @Test
    void testProcessPaymentWithNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepPay.processPayment(-100.0, "USD", "4111111111111111", "12", "2025", "John Doe", "123", "test-id");
        });
    }

    @Test
    void testProcessPaymentWithNullMonth() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepPay.processPayment(100.0, "USD", "4111111111111111", null, "2025", "John Doe", "123", "test-id");
        });
    }

    @Test
    void testProcessPaymentWithNullYear() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepPay.processPayment(100.0, "USD", "4111111111111111", "12", null, "John Doe", "123", "test-id");
        });
    }

    @Test
    void testProcessPaymentWithNullCVV() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepPay.processPayment(100.0, "USD", "4111111111111111", "12", "2025", "John Doe", null, "test-id");
        });
    }

    @Test
    void testProcessPaymentWithNullId() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepPay.processPayment(100.0, "USD", "4111111111111111", "12", "2025", "John Doe", "123", null);
        });
    }

    @Test 
    void testCancelPaymentWithNegativeTransactionId() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepPay.cancelPayment(-12345);
        });
    }
    
}
