package DBLayerTests;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.DBLayer.Message.MessageRepositoryDBImpl;
import com.example.app.DomainLayer.Message;
import com.example.app.SimpleHttpServerApplication;

/**
 * Comprehensive tests for MessageRepositoryDBImpl.
 */
@SpringBootTest(classes = SimpleHttpServerApplication.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
public class MessageRepositoryDBImplTests {

    @Autowired
    private MessageRepositoryDBImpl repo;

    @BeforeEach
    void setUp() {
        // Ensure a clean slate before each test
        repo.getAllMessages().forEach(m -> repo.deleteMessage(m.getMessageId(), m.getSenderId()));
    }

    @Test
    void testAddAndGetMessage() {
        // Add a message
        repo.addMessage(1, 2, "Hello", "2025-06-16T12:00:00", true, -1);
        List<Message> all = repo.getAllMessages();
        assertEquals(1, all.size(), "Should have one message stored");

        Message m = all.get(0);
        assertEquals(1, m.getSenderId());
        assertEquals(2, m.getReceiverId());
        assertEquals("Hello", m.getContent());
        assertEquals("2025-06-16T12:00:00", m.getTimestamp());
        assertTrue(m.isUserToUser());
        assertEquals(-1, m.getPreviousMessageId());

        // get by ID
        Message same = repo.getMessageById(m.getMessageId());
        assertEquals(m, same);
    }

    @Test
    void testGetMessagesBySenderAndReceiver() {
        repo.addMessage(10, 20, "A", "t1", false, -1);
        repo.addMessage(10, 30, "B", "t2", true, -1);
        repo.addMessage(40, 20, "C", "t3", false, -1);

        List<Message> from10 = repo.getMessagesBySenderId(10);
        assertEquals(2, from10.size());
        assertTrue(from10.stream().allMatch(m -> m.getSenderId() == 10));

        List<Message> to20 = repo.getMessagesByReceiverId(20);
        assertEquals(2, to20.size());
        assertTrue(to20.stream().allMatch(m -> m.getReceiverId() == 20));
    }

    @Test
    void testConversationChainAndPrevious() {
        // Build a chain: M1 -> M2 -> M3
        repo.addMessage(1, 2, "M1", "t1", true, -1);
        Message m1 = repo.getAllMessages().get(0);
        repo.addMessage(2, 1, "M2", "t2", true, m1.getMessageId());
        Message m2 = repo.getAllMessages().stream()
                         .filter(m -> m.getPreviousMessageId() == m1.getMessageId())
                         .findFirst().orElseThrow();
        repo.addMessage(1, 2, "M3", "t3", true, m2.getMessageId());
        Message m3 = repo.getAllMessages().stream()
                         .filter(m -> m.getPreviousMessageId() == m2.getMessageId())
                         .findFirst().orElseThrow();

        // previous
        assertEquals(m2, repo.getPreviousMessage(m3.getMessageId()));
        assertNull(repo.getPreviousMessage(-1));

        // full conversation from M3
        List<Message> conv = repo.getFullConversation(m3.getMessageId());
        assertEquals(3, conv.size());
        assertEquals(List.of(m1, m2, m3), conv);
    }

    @Test
    void testIsMessagePrevious() {
        repo.addMessage(5, 6, "X", "tx", false, -1);
        Message mx = repo.getAllMessages().get(0);

        // no previous
        assertTrue(repo.isMessagePrevious(-1, 5, 6));
        // correct pair
        assertTrue(repo.isMessagePrevious(mx.getMessageId(), 5, 6));
        // wrong pair
        assertFalse(repo.isMessagePrevious(mx.getMessageId(), 5, 7));
    }

    @Test
    void testUpdateMessage() {
        repo.addMessage(7, 8, "Old", "t0", false, -1);
        Message mo = repo.getAllMessages().get(0);

        repo.updateMessage(mo.getMessageId(), "New", "t1");
        Message updated = repo.getMessageById(mo.getMessageId());
        assertNotNull(updated);
        assertEquals("New", updated.getContent());
        assertEquals("t1", updated.getTimestamp());
    }

    @Test
    void testDeleteMessage() {
        repo.addMessage(9, 10, "ToDelete", "td", true, -1);
        Message md = repo.getAllMessages().get(0);

        // wrong user cannot delete
        repo.deleteMessage(md.getMessageId(), 999);
        assertNotNull(repo.getMessageById(md.getMessageId()));

        // correct user deletes
        repo.deleteMessage(md.getMessageId(), 9);
        assertNull(repo.getMessageById(md.getMessageId()));
    }
}
