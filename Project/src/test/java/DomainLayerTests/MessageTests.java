package DomainLayerTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.Message;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    private Message message;

    @BeforeEach
    void setUp() {
        message = new Message(1, 100, 200, "Hello!", "2025-04-29 12:00:00", true, 0);
    }

    @Test
    void testConstructorAndGetters() {
        assertEquals(1, message.getMessageId());
        assertEquals(100, message.getSenderId());
        assertEquals(200, message.getReceiverId());
        assertEquals("Hello!", message.getContent());
        assertEquals("2025-04-29 12:00:00", message.getTimestamp());
        assertTrue(message.isUserToUser());
        assertEquals(0, message.getPreviousMessageId());
        assertFalse(message.isDeleted());
    }

    @Test
    void testDeleteSetsIsDeletedTrue() {
        assertFalse(message.isDeleted());
        message.delete();
        assertTrue(message.isDeleted());
    }

    @Test
    void testDeleteIsThreadSafe() throws InterruptedException {
        Thread t1 = new Thread(message::delete);
        Thread t2 = new Thread(message::delete);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertTrue(message.isDeleted());
    }

    @Test
    void testToStringContainsExpectedFields() {
        String output = message.toString();
        assertTrue(output.contains("senderId=100"));
        assertTrue(output.contains("content='Hello!'"));
        assertTrue(output.contains("timestamp='2025-04-29 12:00:00'"));
    }
}
