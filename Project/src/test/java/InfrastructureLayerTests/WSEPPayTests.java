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

import com.example.app.InfrastructureLayer.WSEPPay;

@ExtendWith(MockitoExtension.class)
public class WSEPPayTests {

    @Mock
    private RestTemplate restTemplate;

    private WSEPPay wsepPay;

    @BeforeEach
    public void setUp() {
        wsepPay = new WSEPPay(restTemplate);
    }

    @Test
    public void testIsPaymentServiceAvailable() {
        // Mock the RestTemplate to return "OK"
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("OK");
        
        boolean isAvailable = wsepPay.isPaymentServiceAvailable();
        assertTrue(isAvailable, "Payment service should be available");
    }

    @Test
    public void testIsPaymentServiceUnavailable() {
        // Mock the RestTemplate to return null (service unavailable)
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn(null);
        
        assertThrows(RuntimeException.class, () -> {
            wsepPay.isPaymentServiceAvailable();
        }, "Should throw RuntimeException when service is unavailable");
    }

    @Test
    public void testProcessPaymentSuccess() throws Exception {
        // Mock the RestTemplate to return a valid payment ID
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("12345");
        
        double amount = 1000.5;
        String currency = "USD";
        String cardNumber = "2222333344445555"; // Valid Visa card number
        String expirationDateMonth = "4";
        String expirationDateYear = "2021";
        String cardHolderName = "Israel Israelovice";
        String cvv = "262";
        String id = "20444444";

        int result = wsepPay.processPayment(amount, currency, cardNumber, expirationDateMonth, expirationDateYear, cardHolderName, cvv, id);
        assertEquals(12345, result, "Payment ID should match the mocked value");
    }

    @Test
    public void testProcessPaymentFailure() throws Exception {
        // Mock the RestTemplate to return invalid response
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("invalid");
        
        double amount = 1000.5;
        String currency = "USD";
        String cardNumber = "2222333344445555";
        String expirationDateMonth = "4";
        String expirationDateYear = "2021";
        String cardHolderName = "Israel Israelovice";
        String cvv = "262";
        String id = "20444444";

        int result = wsepPay.processPayment(amount, currency, cardNumber, expirationDateMonth, expirationDateYear, cardHolderName, cvv, id);
        assertEquals(-1, result, "Payment should fail and return -1");
    }

    @Test
    public void testCancelPaymentSuccess() {
        // Mock successful payment first
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn("12345") // First call for processPayment
            .thenReturn("1");    // Second call for cancelPayment
        
        double amount = 100.0; 
        String currency = "USD";
        String cardNumber = "4111111111111111"; // Valid Visa card number
        String expirationDateMonth = "12";
        String expirationDateYear = "2025";
        String cardHolderName = "John Doe";
        String cvv = "123";
        String id = "test-id";

        int result = wsepPay.processPayment(amount, currency, cardNumber, expirationDateMonth, expirationDateYear, cardHolderName, cvv, id);
        assertEquals(12345, result);
        boolean cancelResult = wsepPay.cancelPayment(result);
        assertTrue(cancelResult, "Payment cancellation should be successful");
    }

    @Test
    public void testCancelPaymentFailure() {
        // Mock the RestTemplate to return failure for cancellation
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("0");
        
        int paymentId = 12345;
        boolean cancelResult = wsepPay.cancelPayment(paymentId);
        assertFalse(cancelResult, "Payment cancellation should fail");
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
