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

import com.example.app.InfrastructureLayer.WSEPShipping;
import java.io.IOException;

public class WSEPShippingTests {

    private WSEPShipping wsepShipping;

    @BeforeEach
    public void setUp() {
        wsepShipping = new WSEPShipping();
    }

    @Test
    public void testIsShippingServiceAvailable() {
        try {
            boolean isAvailable = wsepShipping.isShippingServiceAvailable();
            assertTrue(isAvailable, "Shipping service should be available");
        } catch (RuntimeException e) {
            assertFalse(true, "Shipping service is not available: " + e.getMessage());
        }
    }

    @Test
    public void testProcessShippingSuccess() throws Exception {
        String name = "John Doe";
        String address = "123 Main St";
        String city = "Springfield";
        String country = "USA";
        String zipCode = "12345";
        int shippingId = wsepShipping.processShipping(name, address, city, country, zipCode);
        assertNotEquals(0, shippingId, "Shipping ID should not be zero");
        assertTrue(shippingId > 0, "Shipping ID should be a positive integer");
        assertTrue(shippingId >= 10000 && shippingId <= 100000, "Expected shipping ID to be 1 for this test case");
    }

    @Test
    public void testProcessShippingFailure() {
        String name = null; // Invalid input
        String address = "123 Main St";
        String city = "Springfield";
        String country = "USA";
        String zipCode = "12345";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wsepShipping.processShipping(name, address, city, country, zipCode);
        });
        assertEquals("All shipping details must be provided", exception.getMessage(), "Expected exception message for invalid input");
    }

    @Test
    public void testCancelShippingSuccess() {
        int shippingId = 1; // Assuming this is a valid shipping ID
        boolean result = wsepShipping.cancelShipping(shippingId);
        assertTrue(result, "Shipping cancellation should be successful");
    }

    @Test
    public void testCancelShippingFailure() {
        int shippingId = -1; // Invalid shipping ID
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wsepShipping.cancelShipping(shippingId);
        });
        assertEquals("Invalid shipping ID", exception.getMessage(), "Expected exception message for invalid shipping ID");
    }

    @Test
    public void testProcessShippingWithNullName() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepShipping.processShipping(null, "123 Main St", "Springfield", "USA", "12345");
        }, "Expected IllegalArgumentException for null name");
    }

    @Test
    public void testProcessShippingWithNullAddress() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepShipping.processShipping("John Doe", null, "Springfield", "USA", "12345");
        }, "Expected IllegalArgumentException for null address");
    }

    @Test
    public void testProcessShippingWithNullCity() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepShipping.processShipping("John Doe", "123 Main St", null, "USA", "12345");
        }, "Expected IllegalArgumentException for null city");
    }

    @Test
    public void testProcessShippingWithNullCountry() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepShipping.processShipping("John Doe", "123 Main St", "Springfield", null, "12345");
        }, "Expected IllegalArgumentException for null country");
    }

    @Test
    public void testProcessShippingWithNullZipCode() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepShipping.processShipping("John Doe", "123 Main St", "Springfield", "USA", null);
        }, "Expected IllegalArgumentException for null zip code");
    }

    @Test
    public void testCancelShippingWithInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> {
            wsepShipping.cancelShipping(-1); // Invalid shipping ID
        }, "Expected IllegalArgumentException for invalid shipping ID");
    }
}