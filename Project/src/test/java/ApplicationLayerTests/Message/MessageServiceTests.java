package ApplicationLayerTests.Message;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.Message.MessageService;
import com.example.app.ApplicationLayer.Shop.ShopService;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.IMessageRepository;
import com.example.app.DomainLayer.Message;
import com.example.app.DomainLayer.Shop.Shop;

public class MessageServiceTests {

    private IMessageRepository messageRepository;
    private AuthTokenService authTokenService;
    private UserService userService;
    private ShopService shopService;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageRepository = mock(IMessageRepository.class);
        authTokenService = mock(AuthTokenService.class);
        userService = mock(UserService.class);
        shopService = mock(ShopService.class);
        messageService = new MessageService(messageRepository,authTokenService, userService, shopService);
    }

    @Test
    void sendMessageToUser_validMessage_success() throws Exception {
        when(authTokenService.ValidateToken("token")).thenReturn(1);
        when(messageRepository.isMessagePrevious(0, 1, 2)).thenReturn(true);

        String result = messageService.sendMessageToUser("token", 2, "Hello", 0);

        assertEquals("Message sent successfully!", result);
        verify(messageRepository).addMessage(eq(1), eq(2), eq("Hello"), any(), eq(true), eq(0));
    }

    @Test
    void sendMessageToUser_invalidPreviousMessage_returnsError() throws Exception {
        when(authTokenService.ValidateToken("token")).thenReturn(1);
        when(messageRepository.isMessagePrevious(10, 1, 2)).thenReturn(false);

        String result = messageService.sendMessageToUser("token", 2, "Hello", 10);

        assertTrue(result.contains("Previous message with ID 10 isn't proper previous message."));
    }

    @Test
    void sendMessageToShop_validMessage_success() throws Exception {
        Shop shop = mock(Shop.class);
        when(authTokenService.ValidateToken("token")).thenReturn(1);
        when(shopService.getShop(3, "token")).thenReturn(shop);
        when(messageRepository.isMessagePrevious(0, 1, 3)).thenReturn(true);

        String result = messageService.sendMessageToShop("token", 3, "Shop message", 0);

        assertEquals("Message sent successfully!", result);
        verify(messageRepository).addMessage(eq(1), eq(3), eq("Shop message"), any(), eq(false), eq(0));
    }

    @Test
    void sendMessageToShop_shopNotFound_returnsError() throws Exception {
        when(authTokenService.ValidateToken("token")).thenReturn(1);
        when(shopService.getShop(3, "token")).thenReturn(null);

        String result = messageService.sendMessageToShop("token", 3, "Shop message", 0);

        assertTrue(result.contains("Shop with ID 3 doesn't exist."));
    }

    @Test
    void deleteMessage_validUser_success() throws Exception {
        when(authTokenService.ValidateToken("token")).thenReturn(1);

        String result = messageService.deleteMessage("token", 7);

        assertEquals("Message deleted successfully!", result);
        verify(messageRepository).deleteMessage(7, 1);
    }

    @Test
    void getFullConversation_deletedMessage_showsDeletedNote() {
        Message deleted = mock(Message.class);
        when(deleted.isDeleted()).thenReturn(true);

        Message active = mock(Message.class);
        when(active.isDeleted()).thenReturn(false);
        when(active.toString()).thenReturn("Hello");

        when(messageRepository.getFullConversation(1)).thenReturn(Arrays.asList(deleted, active));

        String result = messageService.getFullConversation("token", 1);

        assertTrue(result.contains("Message deleted"));
        assertTrue(result.contains("Hello"));
    }

    @Test
    void getMessageById_notFound_returnsNotFound() {
        when(messageRepository.getMessageById(5)).thenReturn(null);

        String result = messageService.getMessageById("token", 5);

        assertEquals("Message not found!", result);
    }

    @Test
    void getPreviousMessage_found_returnsMessage() {
        Message m = mock(Message.class);
        when(m.toString()).thenReturn("Previous Msg");
        when(messageRepository.getPreviousMessage(3)).thenReturn(m);

        String result = messageService.getPreviousMessage("token", 3);

        assertEquals("Previous Msg", result);
    }

    @Test
    void getPreviousMessage_notFound_returnsMessage() {
        when(messageRepository.getPreviousMessage(999)).thenReturn(null);

        String result = messageService.getPreviousMessage("token", 999);

        assertEquals("No previous message found!", result);
    }

    @Test
    void updateMessage_success() {
        assertDoesNotThrow(() -> messageService.updateMessage("token", 1, "New content"));
        verify(messageRepository).updateMessage(eq(1), eq("New content"), any());
    }

    @Test
    void concurrentSendMessages_threadSafetyCheck() throws Exception {
        when(authTokenService.ValidateToken(anyString())).thenReturn(1);
        when(messageRepository.isMessagePrevious(anyInt(), anyInt(), anyInt())).thenReturn(true);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        Callable<String> task = () -> messageService.sendMessageToUser("token", 2, "Hey", 0);

        List<Future<String>> futures = executor.invokeAll(Collections.nCopies(20, task));
        for (Future<String> f : futures) {
            assertEquals("Message sent successfully!", f.get());
        }

        verify(messageRepository, times(20)).addMessage(eq(1), eq(2), eq("Hey"), any(), eq(true), eq(0));
        executor.shutdown();
    }
}