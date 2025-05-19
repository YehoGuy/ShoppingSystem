package ApplicationLayerTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Item.ItemService;
import com.example.app.ApplicationLayer.Message.MessageService;
import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.ApplicationLayer.Shop.ShopService;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.IMessageRepository;
import com.example.app.DomainLayer.Message;
import com.example.app.DomainLayer.Shop.PurchasePolicy;
import com.example.app.InfrastructureLayer.AuthTokenRepository;
import com.example.app.InfrastructureLayer.ItemRepository;
import com.example.app.InfrastructureLayer.MessageRepository;
import com.example.app.InfrastructureLayer.ShopRepository;
import com.example.app.InfrastructureLayer.UserRepository;


public class MessageServiceTests {

    private MessageService messageService;
    private AuthTokenService authTokenService;
    private UserService userService;
    private ShopService shopService;
    private AuthTokenRepository authTokenRepository;
    private UserRepository userRepository;
    private ShopRepository shopRepository;
    private String token1;

    @Mock
    private ShippingMethod shippingMethodMock;
    @Mock
    private PurchasePolicy purchasePolicyMock;
    
    @BeforeEach
    void setUp() {
        // Initialize the message service and repository before each test
        authTokenRepository = new AuthTokenRepository();
        authTokenService = new AuthTokenService(authTokenRepository);
        userRepository = new UserRepository();
        userService = new UserService(userRepository, authTokenService);
        shopRepository = new ShopRepository();
        shopService = new ShopService(shopRepository, authTokenService, userService, new ItemService(new ItemRepository(),authTokenService,userService));
        messageService = new MessageService(new MessageRepository(),authTokenService, userService, shopService);

        // Add a test user and shop to the repositories
        userRepository.addMember("testUser", "password", "a@a", "b", "c");
        userRepository.addMember("testUser2", "password", "a@a", "b", "c");
        token1 = authTokenService.Login("testUser", "password", 1);
        shopService.createShop("shop1", purchasePolicyMock, shippingMethodMock, token1);
    }

    @Test
    void testSendMessageToUser() {
        // Test sending a message to a user
        assertEquals("Message sent successfully!", messageService.sendMessageToUser(token1, 2, "Hello", 0));
    }

    @Test
    void testSendMessageToShop() {
        // Test sending a message to a shop
        assertEquals("Message sent successfully!", messageService.sendMessageToShop(token1, 1, "Hello", 0));
    }

    @Test
    void testSendMessageToUserWithInvalidToken() {
        // Test sending a message with an invalid token
        assertEquals("Error sending message to user: Invalid token", messageService.sendMessageToUser("invalidToken", 2, "Hello", 0));
    }

    @Test
    void testSendMessageToUserWithInvalidReceiverId() {
        // Test sending a message to an invalid receiver ID
        assertEquals("Error sending message to user: IssacTheDebugException thrown! mesage: User with ID 9999 doesn't exist. objects involved: []", messageService.sendMessageToUser(token1, 9999, "Hello", 0));
    }

    @Test
    void testSendMessageToUserWithInvalidPreviousMessageId() {
        // Test sending a message with an invalid previous message ID
        assertEquals("Error sending message to user: MosheTheDebugException thrown! mesage: Previous message with ID 9999 isn't proper previous message. objects involved: []", messageService.sendMessageToUser(token1, 2, "Hello", 9999));
    }

    @Test
    void testSendMessageToShopWithEmptyText() {
        // Test sending a message with empty text to a shop
        assertEquals("Error sending message to shop: MosheTheDebugException thrown! mesage: unable to send - message is empty. objects involved: []", messageService.sendMessageToShop(token1, 1, "", 0));
    }

    @Test
    void testSendMessageToShopWithInvalidToken() {
        // Test sending a message to a shop with an invalid token
        assertEquals("Error sending message to shop: Invalid token", messageService.sendMessageToShop("invalidToken", 1, "Hello", 0));
    }

    @Test
    void testSendMessageToShopWithInvalidReceiverId() {
        // Test sending a message to an invalid receiver ID
        assertEquals("Error sending message to shop: MosheTheDebugException thrown! message: Error retrieving shop with id 9999: Error retrieving shop: Shop not found: 9999", messageService.sendMessageToShop(token1, 9999, "Hello", 0));
    }

    // ----- deleteMessage -----

    @Test
    void testDeleteMessage_Success() throws Exception {
        // first send a message so there is something to delete
        messageService.sendMessageToUser(token1, 2, "Hi", 0); // creates id=1
        assertEquals("Message deleted successfully!",
                     messageService.deleteMessage(token1, 1));
    }

    @Test
    void testDeleteMessage_InvalidToken() {
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> messageService.deleteMessage("badToken", 1));
        assertTrue(ex.getMessage().contains("Error deleting message: Invalid token"));
    }

    @Test
    void testDeleteMessage_NotFoundOrUnauthorized() {
        // deleting a non-existent or not-yours message should return an error string
        String result = messageService.deleteMessage(token1, 999);
        assertTrue(result.startsWith("Error deleting message:"),
                   "Expected an error message when deleting a missing or unauthorized message");
    }

    // ----- updateMessage -----

    @Test
    void testUpdateMessage_Success() throws Exception {
        messageService.sendMessageToUser(token1, 2, "Hello", 0); // id=1
        assertEquals("Message updated successfully!",
                     messageService.updateMessage(token1, 1, "Updated content"));
    }

    @Test
    void testUpdateMessage_NotFound() {
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> messageService.updateMessage(token1, 999, "Content"));
        assertTrue(ex.getMessage().contains("Error updating message"));
    }

    // ----- getFullConversation -----

    @Test
    void testGetFullConversation_Success() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        Message m1 = mock(Message.class), m2 = mock(Message.class), m3 = mock(Message.class);
        when(m1.isDeleted()).thenReturn(false);
        when(m1.toString()).thenReturn("m1");
        when(m2.isDeleted()).thenReturn(true);
        when(m3.isDeleted()).thenReturn(false);
        when(m3.toString()).thenReturn("m3");
        when(repoMock.getFullConversation(10)).thenReturn(List.of(m1, m2, m3));

        MessageService svc = new MessageService(repoMock, null, null, null);
        String out = svc.getFullConversation("any", 10);

        assertTrue(out.startsWith("Full conversation:"));
        assertTrue(out.contains("m1"));
        assertTrue(out.contains("Message deleted"));
        assertTrue(out.contains("m3"));
    }

    @Test
    void testGetFullConversation_RepoThrowsOurArg() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getFullConversation(anyInt())).thenThrow(new OurArg("bad"));
        MessageService svc = new MessageService(repoMock, null, null, null);

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> svc.getFullConversation("any", 1));
        assertTrue(ex.getMessage().contains("getFullConversation"));
    }

    @Test
    void testGetFullConversation_RepoThrowsOther() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getFullConversation(anyInt())).thenThrow(new RuntimeException("oops"));
        MessageService svc = new MessageService(repoMock, null, null, null);

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> svc.getFullConversation("any", 1));
        assertTrue(ex.getMessage().contains("Error getting full conversation: oops"));
    }

    // ----- getMessagesBySenderId -----

    // @Test
    // void testGetMessagesBySenderId_Success() {
    //     IMessageRepository repoMock = mock(IMessageRepository.class);
    //     Message m = mock(Message.class);
    //     when(m.toString()).thenReturn("sentMsg");
    //     when(repoMock.getMessagesBySenderId(7)).thenReturn(List.of(m));

    //     MessageService svc = new MessageService(repoMock, null, null, null);
    //     String out = svc.getMessagesBySenderId("any", 7);

    //     assertTrue(out.startsWith("Messages sent by user 7:"));
    //     assertTrue(out.contains("sentMsg"));
    // }

    @Test
    void testGetMessagesBySenderId_RepoThrowsOurArg() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getMessagesBySenderId(anyInt())).thenThrow(new OurArg("err"));

        MessageService svc = new MessageService(repoMock, null, null, null);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> svc.getMessagesBySenderId("any", 1));
        assertTrue(ex.getMessage().contains("getMessagesBySenderId"));
    }

    @Test
    void testGetMessagesBySenderId_RepoThrowsOther() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getMessagesBySenderId(anyInt())).thenThrow(new RuntimeException("oops"));

        MessageService svc = new MessageService(repoMock, null, null, null);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> svc.getMessagesBySenderId("any", 1));
        assertTrue(ex.getMessage().contains("Error getting messages by sender ID"));
    }

    // ----- getMessagesByReceiverId -----

    // @Test
    // void testGetMessagesByReceiverId_Success() {
    //     IMessageRepository repoMock = mock(IMessageRepository.class);
    //     Message m = mock(Message.class);
    //     when(m.toString()).thenReturn("recvMsg");
    //     when(repoMock.getMessagesByReceiverId(8)).thenReturn(List.of(m));

    //     MessageService svc = new MessageService(repoMock, null, null, null);
    //     String out = svc.getMessagesByReceiverId("any", 8);

    //     assertTrue(out.startsWith("Messages received by user 8:"));
    //     assertTrue(out.contains("recvMsg"));
    // }

    @Test
    void testGetMessagesByReceiverId_RepoThrowsOurArg() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getMessagesByReceiverId(anyInt())).thenThrow(new OurArg("bad"));

        MessageService svc = new MessageService(repoMock, null, null, null);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> svc.getMessagesByReceiverId("any", 1));
        assertTrue(ex.getMessage().contains("getMessagesByReceiverId"));
    }

    @Test
    void testGetMessagesByReceiverId_RepoThrowsOther() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getMessagesByReceiverId(anyInt())).thenThrow(new RuntimeException("oops"));

        MessageService svc = new MessageService(repoMock, null, null, null);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> svc.getMessagesByReceiverId("any", 1));
        assertTrue(ex.getMessage().contains("Error getting messages by receiver ID"));
    }

    // ----- getMessageById -----

    @Test
    void testGetMessageById_NotFound() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getMessageById(5)).thenReturn(null);

        MessageService svc = new MessageService(repoMock, null, null, null);
        assertEquals("Message not found!", svc.getMessageById("any", 5));
    }

    @Test
    void testGetMessageById_Success() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        Message m = mock(Message.class);
        when(m.toString()).thenReturn("foundMsg");
        when(repoMock.getMessageById(6)).thenReturn(m);

        MessageService svc = new MessageService(repoMock, null, null, null);
        assertEquals("foundMsg", svc.getMessageById("any", 6));
    }

    @Test
    void testGetMessageById_RepoThrowsOurArg() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getMessageById(anyInt())).thenThrow(new OurArg("err"));

        MessageService svc = new MessageService(repoMock, null, null, null);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> svc.getMessageById("any", 1));
        assertTrue(ex.getMessage().contains("getMessageById"));
    }

    @Test
    void testGetMessageById_RepoThrowsOther() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getMessageById(anyInt())).thenThrow(new RuntimeException("oops"));

        MessageService svc = new MessageService(repoMock, null, null, null);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> svc.getMessageById("any", 1));
        assertTrue(ex.getMessage().contains("Error getting message by ID"));
    }

    // ----- getPreviousMessage -----

    @Test
    void testGetPreviousMessage_NotFound() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getPreviousMessage(9)).thenReturn(null);

        MessageService svc = new MessageService(repoMock, null, null, null);
        assertEquals("No previous message found!",
                     svc.getPreviousMessage("any", 9));
    }

    @Test
    void testGetPreviousMessage_Success() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        Message m = mock(Message.class);
        when(m.toString()).thenReturn("prevMsg");
        when(repoMock.getPreviousMessage(10)).thenReturn(m);

        MessageService svc = new MessageService(repoMock, null, null, null);
        assertEquals("prevMsg", svc.getPreviousMessage("any", 10));
    }

    @Test
    void testGetPreviousMessage_RepoThrowsOurArg() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getPreviousMessage(anyInt())).thenThrow(new OurArg("err"));

        MessageService svc = new MessageService(repoMock, null, null, null);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> svc.getPreviousMessage("any", 1));
        assertTrue(ex.getMessage().contains("getPreviousMessage"));
    }

    @Test
    void testGetPreviousMessage_RepoThrowsOther() {
        IMessageRepository repoMock = mock(IMessageRepository.class);
        when(repoMock.getPreviousMessage(anyInt())).thenThrow(new RuntimeException("oops"));

        MessageService svc = new MessageService(repoMock, null, null, null);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> svc.getPreviousMessage("any", 1));
        assertTrue(ex.getMessage().contains("Error getting previous message"));
    }
}
