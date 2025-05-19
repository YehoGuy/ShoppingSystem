package InfrastructureLayerTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DomainLayer.Message;
import com.example.app.InfrastructureLayer.MessageRepository;

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
    void testAddMessageAndGetAll() {
        repo.addMessage(1, 2, "Hello", "ts", true, 0);
        repo.addMessage(2, 1, "Reply", "ts2", true, 1);
        List<Message> all = repo.getAllMessages();
        assertEquals(2, all.size());
    }

    @Test
    void testAddMessage_EmptyOrNull_Throws() {
        assertThrows(OurRuntime.class, () -> repo.addMessage(1,2,"","ts",true,0));
        assertThrows(OurRuntime.class, () -> repo.addMessage(1,2,null,"ts",true,0));
    }

    @Test
    void testGetMessageById_ExistingAndMissing() {
        repo.addMessage(1,2,"X","t",true,0);
        Message m = repo.getMessageById(1);
        assertNotNull(m);
        assertNull(repo.getMessageById(99));
    }

    @Test
    void testDeleteMessage_SuccessAndFailure() {
        repo.addMessage(1,2,"Y","t",true,0);
        repo.deleteMessage(1,1);
        assertTrue(repo.getMessageById(1).isDeleted());
        assertThrows(OurRuntime.class, () -> repo.deleteMessage(999,1));
        repo.addMessage(1,2,"Z","t",true,0);
        assertThrows(OurRuntime.class, () -> repo.deleteMessage(2,99));
    }

    @Test
    void testUpdateMessage_SuccessAndFailure() {
        repo.addMessage(1,2,"A","t",true,0);
        repo.updateMessage(1,"A2","t2");
        assertEquals("A2", repo.getMessageById(1).getContent());
        assertThrows(IndexOutOfBoundsException.class, () -> repo.updateMessage(99,"X","t"));
    }

    @Test
    void testGetMessagesBySenderAndReceiver() {
        repo.addMessage(1,2,"S1","t",true,0);
        repo.addMessage(1,3,"S2","t",true,0);
        repo.addMessage(2,1,"R1","t",true,0);
        assertEquals(2, repo.getMessagesBySenderId(1).size());
        assertEquals(1, repo.getMessagesByReceiverId(1).size());
    }

    // @Test
    // void testPreviousAndConversationAndIsMessagePrevious() {
    //     repo.addMessage(1,2,"First","t",true,0); // id=1
    //     repo.addMessage(2,1,"Second","t2",true,1); // id=2
    //     // previous
    //     assertEquals(repo.getMessageById(1), repo.getPreviousMessage(2));
    //     assertNull(repo.getPreviousMessage(1));
    //     // isMessagePrevious
    //     assertTrue(repo.isMessagePrevious(0,1,2));
    //     assertTrue(repo.isMessagePrevious(1,1,2));
    //     assertFalse(repo.isMessagePrevious(1,2,3));
    //     // full conversation
    //     List<Message> convo = repo.getFullConversation(2);
    //     assertEquals(2, convo.size());
    //     // if we delete the first, conversation shrinks
    //     repo.deleteMessage(1,1);
    //     List<Message> convo2 = repo.getFullConversation(2);
    //     assertEquals(1, convo2.size());
    // }
}
