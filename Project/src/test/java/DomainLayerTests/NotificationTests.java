package DomainLayerTests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.Notification;

public class NotificationTests {

    @Test
    public void testNotificationGetSetAndToString() {
        Notification n = new Notification("T","M");
        assertEquals("T", n.getTitle());
        assertEquals("M", n.getMessage());

        n.setTitle("T2");
        n.setMessage("M2");
        assertTrue(n.toString().contains("T2"));
        assertTrue(n.toString().contains("M2"));
    }
}
