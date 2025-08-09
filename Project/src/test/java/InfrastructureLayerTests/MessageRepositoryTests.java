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

    /* ═══════════════════ Enhanced tests for specific functions ═══════════════════ */

    @Test
    void testGetMessageById_ComprehensiveScenarios() {
        // Test with existing message
        repo.addMessage(1, 2, "Test message", "2024-04-30", true, -1);
        Message existing = repo.getMessageById(1);
        assertNotNull(existing);
        assertEquals(1, existing.getMessageId());
        assertEquals("Test message", existing.getContent());

        // Test with non-existent message - should return null
        Message nonExistent = repo.getMessageById(999);
        assertNull(nonExistent);

        // Test with zero ID
        Message zeroId = repo.getMessageById(0);
        assertNull(zeroId);

        // Test with negative ID
        Message negativeId = repo.getMessageById(-1);
        assertNull(negativeId);

        // Test with deleted message - should still return the message object
        repo.addMessage(1, 2, "To be deleted", "2024-04-30", true, -1);
        int messageToDeleteId = 2; // This will be the next ID
        repo.deleteMessage(messageToDeleteId, 1);
        Message deletedMessage = repo.getMessageById(messageToDeleteId);
        assertNotNull(deletedMessage, "getMessageById should return deleted messages");
        assertTrue(deletedMessage.isDeleted(), "Message should be marked as deleted");
    }

    @Test
    void testGetPreviousMessage_ComprehensiveScenarios() {
        // Test message with no previous message (previousMessageId = -1)
        repo.addMessage(1, 2, "First message", "T1", true, -1); // id=1
        Message noPrevious = repo.getPreviousMessage(1);
        assertNull(noPrevious, "Should return null when previousMessageId is -1");

        // Test message with valid previous message
        repo.addMessage(2, 1, "Second message", "T2", true, 1); // id=2, previous=1
        Message hasPrevious = repo.getPreviousMessage(2);
        assertNotNull(hasPrevious);
        assertEquals(1, hasPrevious.getMessageId());
        assertEquals("First message", hasPrevious.getContent());

        // Test chain of messages
        repo.addMessage(1, 2, "Third message", "T3", true, 2); // id=3, previous=2
        Message chainedPrevious = repo.getPreviousMessage(3);
        assertNotNull(chainedPrevious);
        assertEquals(2, chainedPrevious.getMessageId());
        assertEquals("Second message", chainedPrevious.getContent());

        // Test with non-existent message ID
        Message nonExistentMessage = repo.getPreviousMessage(999);
        assertNull(nonExistentMessage, "Should return null for non-existent message");

        // Test with deleted previous message
        repo.addMessage(1, 2, "To be deleted", "T4", true, -1); // id=4
        repo.addMessage(2, 1, "References deleted", "T5", true, 4); // id=5, previous=4
        repo.deleteMessage(4, 1); // Delete the previous message
        Message deletedPrevious = repo.getPreviousMessage(5);
        assertNull(deletedPrevious, "Should return null when previous message is deleted");

        // Test with invalid previous message ID (points to non-existent message)
        repo.addMessage(1, 2, "Invalid ref", "T6", true, 999); // id=6, previous=999 (doesn't exist)
        Message invalidRef = repo.getPreviousMessage(6);
        assertNull(invalidRef, "Should return null when previous message doesn't exist");
    }

    @Test
    void testIsMessagePrevious_ComprehensiveScenarios() {
        // Test with previousMessageId = -1 (should always return true)
        assertTrue(repo.isMessagePrevious(-1, 1, 2), "Should return true when previousMessageId is -1");
        assertTrue(repo.isMessagePrevious(-1, 999, 888), "Should return true when previousMessageId is -1");

        // Test with valid message that matches sender/receiver in same direction
        repo.addMessage(1, 2, "From 1 to 2", "T1", true, -1); // id=1
        assertTrue(repo.isMessagePrevious(1, 1, 2), "Should return true for correct sender->receiver match");

        // Test with valid message that matches sender/receiver in reverse direction
        assertTrue(repo.isMessagePrevious(1, 2, 1), "Should return true for reverse receiver->sender match");

        // Test with valid message that doesn't match participants
        assertFalse(repo.isMessagePrevious(1, 3, 4), "Should return false when participants don't match");
        assertFalse(repo.isMessagePrevious(1, 1, 3), "Should return false when only sender matches");
        assertFalse(repo.isMessagePrevious(1, 3, 2), "Should return false when only receiver matches");

        // Test with non-existent message ID
        assertFalse(repo.isMessagePrevious(999, 1, 2), "Should return false for non-existent message");

        // Test with complex conversation scenario
        repo.addMessage(10, 20, "Msg 1: 10->20", "T2", true, -1); // id=2
        repo.addMessage(20, 10, "Msg 2: 20->10", "T3", true, 2);  // id=3
        repo.addMessage(10, 20, "Msg 3: 10->20", "T4", true, 3);  // id=4

        assertTrue(repo.isMessagePrevious(2, 10, 20), "Should work in conversation context");
        assertTrue(repo.isMessagePrevious(3, 10, 20), "Should work with reverse direction in conversation");
        assertFalse(repo.isMessagePrevious(2, 10, 30), "Should fail when third party involved");

        // Test edge cases with deleted messages
        repo.addMessage(1, 2, "Will be deleted", "T5", true, -1); // id=5
        repo.deleteMessage(5, 1);
        // Note: isMessagePrevious doesn't check if message is deleted, only if it exists and matches participants
        assertTrue(repo.isMessagePrevious(5, 1, 2), "Should still return true for deleted message if participants match");
    }

    @Test
    void testGetFullConversation_ComprehensiveScenarios() {
        // Test single message with no previous
        repo.addMessage(1, 2, "Single message", "T1", true, -1); // id=1
        List<Message> singleConvo = repo.getFullConversation(1);
        assertEquals(1, singleConvo.size());
        assertEquals("Single message", singleConvo.get(0).getContent());

        // Test chain of messages
        repo.addMessage(1, 2, "Message 1", "T2", true, -1);  // id=2
        repo.addMessage(2, 1, "Message 2", "T3", true, 2);   // id=3, previous=2
        repo.addMessage(1, 2, "Message 3", "T4", true, 3);   // id=4, previous=3
        repo.addMessage(2, 1, "Message 4", "T5", true, 4);   // id=5, previous=4

        List<Message> fullConvo = repo.getFullConversation(5);
        assertEquals(4, fullConvo.size(), "Should include all messages in the chain");
        
        // Verify order (should be newest to oldest)
        assertEquals("Message 4", fullConvo.get(0).getContent());
        assertEquals("Message 3", fullConvo.get(1).getContent());
        assertEquals("Message 2", fullConvo.get(2).getContent());
        assertEquals("Message 1", fullConvo.get(3).getContent());

        // Test with deleted message in chain
        repo.deleteMessage(3, 2); // Delete "Message 2"
        List<Message> convoWithDeleted = repo.getFullConversation(5);
        assertEquals(3, convoWithDeleted.size(), "Should exclude deleted messages");
        
        // Verify deleted message is not included
        boolean foundDeleted = convoWithDeleted.stream()
                .anyMatch(m -> m.getContent().equals("Message 2"));
        assertFalse(foundDeleted, "Deleted message should not be in conversation");

        // Test with non-existent message ID
        List<Message> nonExistentConvo = repo.getFullConversation(999);
        assertTrue(nonExistentConvo.isEmpty(), "Should return empty list for non-existent message");

        // Test with message that has invalid previous message reference
        repo.addMessage(1, 2, "Invalid ref", "T6", true, 888); // id=6, previous=888 (doesn't exist)
        List<Message> invalidRefConvo = repo.getFullConversation(6);
        assertEquals(1, invalidRefConvo.size(), "Should only include the message itself when previous doesn't exist");
        assertEquals("Invalid ref", invalidRefConvo.get(0).getContent());

        // Test conversation starting from middle of chain
        List<Message> middleConvo = repo.getFullConversation(4); // Start from "Message 3"
        assertEquals(2, middleConvo.size(), "Should include messages from start point backwards (excluding deleted)");
        assertEquals("Message 3", middleConvo.get(0).getContent());
        assertEquals("Message 1", middleConvo.get(1).getContent()); // "Message 2" was deleted, so only Message 1 and 3
    }

    @Test
    void testGetPreviousMessageNotMatterWhat_ComprehensiveScenarios() {
        // Note: This is a private method, but we can test its behavior indirectly
        // by understanding how it differs from getPreviousMessage
        // getPreviousMessageNotMatterWhat doesn't check if the previous message is deleted

        // Test message with no previous message (previousMessageId = -1)
        repo.addMessage(1, 2, "First message", "T1", true, -1); // id=1
        
        // Test message with valid previous message
        repo.addMessage(2, 1, "Second message", "T2", true, 1); // id=2, previous=1
        
        // Test with deleted previous message - we can't directly test the private method,
        // but we can verify the difference in behavior between getPreviousMessage and
        // the full conversation functionality
        repo.addMessage(1, 2, "To be deleted", "T3", true, -1); // id=3
        repo.addMessage(2, 1, "References deleted", "T4", true, 3); // id=4, previous=3
        
        // Before deletion
        Message beforeDeletion = repo.getPreviousMessage(4);
        assertNotNull(beforeDeletion, "Should find previous message before deletion");
        
        // After deletion
        repo.deleteMessage(3, 1); // Delete the previous message
        Message afterDeletion = repo.getPreviousMessage(4);
        assertNull(afterDeletion, "getPreviousMessage should return null for deleted previous message");
        
        // The difference in behavior would be visible in getFullConversation
        // which uses the private method internally for chain traversal
        List<Message> conversation = repo.getFullConversation(4);
        assertEquals(1, conversation.size(), "Conversation should only include non-deleted messages");
    }

    @Test
    void testIntegration_AllFunctionsTogether() {
        // Create a complex conversation scenario
        repo.addMessage(10, 20, "Hello", "T1", true, -1);        // id=1
        repo.addMessage(20, 10, "Hi there", "T2", true, 1);      // id=2, previous=1
        repo.addMessage(10, 20, "How are you?", "T3", true, 2);  // id=3, previous=2
        repo.addMessage(20, 10, "Good!", "T4", true, 3);         // id=4, previous=3
        repo.addMessage(10, 20, "Nice!", "T5", true, 4);         // id=5, previous=4

        // Test getMessageById for all messages
        for (int i = 1; i <= 5; i++) {
            Message msg = repo.getMessageById(i);
            assertNotNull(msg, "Message " + i + " should exist");
            assertEquals(i, msg.getMessageId());
        }

        // Test getPreviousMessage for each message in the chain
        assertNull(repo.getPreviousMessage(1), "First message should have no previous");
        assertEquals(1, repo.getPreviousMessage(2).getMessageId());
        assertEquals(2, repo.getPreviousMessage(3).getMessageId());
        assertEquals(3, repo.getPreviousMessage(4).getMessageId());
        assertEquals(4, repo.getPreviousMessage(5).getMessageId());

        // Test isMessagePrevious for valid conversation flow
        assertTrue(repo.isMessagePrevious(-1, 10, 20), "Should accept -1 as previous");
        assertTrue(repo.isMessagePrevious(1, 20, 10), "Should accept reverse direction");
        assertTrue(repo.isMessagePrevious(2, 10, 20), "Should accept same direction");
        assertFalse(repo.isMessagePrevious(1, 10, 30), "Should reject wrong participants");

        // Test getFullConversation from different starting points
        List<Message> fullFromEnd = repo.getFullConversation(5);
        assertEquals(5, fullFromEnd.size(), "Should include all messages when starting from end");

        List<Message> fullFromMiddle = repo.getFullConversation(3);
        assertEquals(3, fullFromMiddle.size(), "Should include first 3 messages when starting from middle");

        // Delete a middle message and test again
        repo.deleteMessage(3, 10); // Delete "How are you?"
        
        List<Message> afterDeletion = repo.getFullConversation(5);
        assertEquals(4, afterDeletion.size(), "Should exclude deleted message from conversation");
        
        // Verify the deleted message is skipped in conversation
        boolean foundDeleted = afterDeletion.stream()
                .anyMatch(m -> m.getContent().equals("How are you?"));
        assertFalse(foundDeleted, "Deleted message should not appear in conversation");

        // But getPreviousMessage should still work for messages after the deleted one
        Message previousToFourth = repo.getPreviousMessage(4);
        assertNull(previousToFourth, "Should return null when previous message is deleted");
    }

    @Test
    void testEdgeCases_ErrorHandling() {
        // Test with empty repository
        assertNull(repo.getMessageById(1), "Should return null for empty repository");
        assertNull(repo.getPreviousMessage(1), "Should return null for empty repository");
        assertFalse(repo.isMessagePrevious(1, 1, 2), "Should return false for empty repository");
        assertTrue(repo.getFullConversation(1).isEmpty(), "Should return empty list for empty repository");

        // Test with very large IDs
        assertNull(repo.getMessageById(Integer.MAX_VALUE), "Should handle max integer ID");
        assertNull(repo.getPreviousMessage(Integer.MAX_VALUE), "Should handle max integer ID");
        
        // Test circular reference prevention (if applicable)
        // Note: The current implementation doesn't prevent circular references,
        // but this test documents the expected behavior
        repo.addMessage(1, 2, "Msg A", "T1", true, -1); // id=1
        repo.addMessage(2, 1, "Msg B", "T2", true, 1);  // id=2, previous=1
        
        // If we could modify the previous message ID to create a circular reference,
        // the system should handle it gracefully. Since we can't modify previousMessageId
        // after creation, this is more of a design documentation test.
        List<Message> normalConvo = repo.getFullConversation(2);
        assertEquals(2, normalConvo.size(), "Should handle normal conversation without infinite loop");
    }
}
