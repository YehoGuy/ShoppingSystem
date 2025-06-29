package InfrastructureLayerTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.example.app.InfrastructureLayer.WSEPShipping;

@ExtendWith(MockitoExtension.class)
public class WSEPShippingTests {

    @Mock
    private RestTemplate restTemplate;

    private WSEPShipping wsepShipping;

    @BeforeEach
    public void setUp() {
        wsepShipping = new WSEPShipping(restTemplate);
    }

    @Test
    public void testIsShippingServiceAvailable() {
        // Mock the RestTemplate to return "OK"
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("OK");
        
        boolean isAvailable = wsepShipping.isShippingServiceAvailable();
        assertTrue(isAvailable, "Shipping service should be available");
    }

    @Test
    public void testIsShippingServiceUnavailable() {
        // Mock the RestTemplate to return null (service unavailable)
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn(null);
        
        assertThrows(RuntimeException.class, () -> {
            wsepShipping.isShippingServiceAvailable();
        }, "Should throw RuntimeException when service is unavailable");
    }

    @Test
    public void testProcessShippingSuccess() throws Exception {
        // Mock the RestTemplate to return a valid shipping ID
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("12345");
        
        String name = "John Doe";
        String address = "123 Main St";
        String city = "Springfield";
        String country = "USA";
        String zipCode = "12345";
        int shippingId = wsepShipping.processShipping(name, address, city, country, zipCode);
        assertEquals(12345, shippingId, "Shipping ID should match the mocked value");
    }

    @Test
    public void testProcessShippingFailureNullResponse() throws Exception {
        // Mock the RestTemplate to return null
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn(null);
        
        String name = "John Doe";
        String address = "123 Main St";
        String city = "Springfield";
        String country = "USA";
        String zipCode = "12345";
        
        assertThrows(RuntimeException.class, () -> {
            wsepShipping.processShipping(name, address, city, country, zipCode);
        }, "Should throw RuntimeException when response is null");
    }

    @Test
    public void testProcessShippingFailureInvalidResponse() throws Exception {
        // Mock the RestTemplate to return invalid response
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("invalid");
        
        String name = "John Doe";
        String address = "123 Main St";
        String city = "Springfield";
        String country = "USA";
        String zipCode = "12345";
        
        assertThrows(RuntimeException.class, () -> {
            wsepShipping.processShipping(name, address, city, country, zipCode);
        }, "Should throw RuntimeException when response is invalid");
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
        // Mock the RestTemplate to return success
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("1");
        
        int shippingId = 12345; // Valid shipping ID
        boolean result = wsepShipping.cancelShipping(shippingId);
        assertTrue(result, "Shipping cancellation should be successful");
    }

    @Test
    public void testCancelShippingFailure() {
        // Mock the RestTemplate to return failure
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("0");
        
        int shippingId = 12345; // Valid shipping ID but cancellation fails
        boolean result = wsepShipping.cancelShipping(shippingId);
        assertFalse(result, "Shipping cancellation should fail");
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

    @Test
    public void testCancelShippingInvalidIdException() {
        int shippingId = -1; // Invalid shipping ID
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wsepShipping.cancelShipping(shippingId);
        });
        assertEquals("Invalid shipping ID", exception.getMessage(), "Expected exception message for invalid shipping ID");
    }
}