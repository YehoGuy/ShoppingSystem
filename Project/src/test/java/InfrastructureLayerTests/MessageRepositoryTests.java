package InfrastructureLayerTests;

import DomainLayer.Message;
import InfrastructureLayer.MessageRepository;
import ApplicationLayer.OurRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MessageRepositoryTests {

    private MessageRepository repo;

    @BeforeEach
    void setUp() {
        repo = new MessageRepository();
    }

    @Test
    void testAddMessageAndGetMessageById() {
        repo.addMessage(1, 2, "Hello", "2024-04-30 10:00", true, -1);
        Message m = repo.getMessageById(1);

        assertNotNull(m);
        assertEquals("Hello", m.getContent());
        assertEquals(1, m.getSenderId());
        assertEquals(2, m.getReceiverId());
        assertEquals(-1, m.getPreviousMessageId());
    }

    @Test
    void testAddMessageWithEmptyContentThrowsException() {
        OurRuntime ex = assertThrows(OurRuntime.class, () -> repo.addMessage(1, 2, "", "2024-04-30 10:01", true, -1));
        assertEquals("MosheTheDebugException thrown! mesage: unable to send - message is empty. objects involved: []",
                ex.getMessage());
    }

    @Test
    void testDeleteMessageByWrongUserThrowsException() {
        repo.addMessage(1, 2, "Message", "2024-04-30", true, -1);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> repo.deleteMessage(1, 3) // senderId doesn't match
        );
        assertEquals(
                "MosheTheDebugException thrown! mesage: You are not authorized to delete this message. objects involved: []",
                ex.getMessage());
    }

    @Test
    void testDeleteAndCheckDeletedFlag() {
        repo.addMessage(1, 2, "Message", "2024-04-30", true, -1);
        repo.deleteMessage(1, 1);
        assertTrue(repo.getMessageById(1).isDeleted());
    }

    @Test
    void testUpdateMessageChangesContent() {
        repo.addMessage(1, 2, "Old Content", "2024-04-30", true, -1);
        repo.updateMessage(1, "New Content", "2024-04-30 12:00");
        Message updated = repo.getMessageById(1);
        assertEquals("New Content", updated.getContent());
    }

    @Test
    void testGetMessagesBySenderId() {
        repo.addMessage(1, 2, "First", "T1", true, -1);
        repo.addMessage(1, 3, "Second", "T2", true, -1);
        repo.addMessage(2, 1, "Third", "T3", true, -1);
        repo.deleteMessage(2, 1); // delete third message

        List<Message> messages = repo.getMessagesBySenderId(1);
        assertEquals(1, messages.size());
    }

    @Test
    void testGetMessagesByReceiverId() {
        repo.addMessage(1, 2, "First", "T1", true, -1);
        repo.addMessage(3, 2, "Second", "T2", true, -1);
        repo.addMessage(4, 1, "Third", "T3", true, -1);
        repo.deleteMessage(3, 4); // delete second message

        List<Message> messages = repo.getMessagesByReceiverId(2);
        assertEquals(2, messages.size());
    }

    @Test
    void testGetPreviousMessage() {
        repo.addMessage(1, 2, "First", "T1", true, -1); // id=1
        repo.addMessage(2, 1, "Second", "T2", true, 1); // id=2

        Message prev = repo.getPreviousMessage(2);
        assertNotNull(prev);
        assertEquals("First", prev.getContent());
    }

    @Test
    void testGetFullConversation() {
        repo.addMessage(1, 2, "First", "T1", true, -1); // id=1
        repo.addMessage(2, 1, "Second", "T2", true, 1); // id=2
        repo.addMessage(1, 2, "Third", "T3", true, 2); // id=3

        List<Message> conversation = repo.getFullConversation(3);
        assertEquals(3, conversation.size());
    }

    @Test
    void testIsMessagePrevious() {
        repo.addMessage(1, 2, "Hi", "T1", true, -1); // id=1
        boolean valid = repo.isMessagePrevious(1, 1, 2);
        assertTrue(valid);

        boolean invalid = repo.isMessagePrevious(1, 3, 4);
        assertFalse(invalid);
    }
}
