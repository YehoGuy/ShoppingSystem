package ApplicationLayerTests.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.OurRuntime;
import ApplicationLayer.Message.MessageService;
import ApplicationLayer.Shop.ShopService;
import ApplicationLayer.User.UserService;
import DomainLayer.IMessageRepository;
import DomainLayer.Shop.Shop;

public class MessageServiceAcceptanceTests {
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
        messageService = new MessageService(messageRepository);
        messageService.setService(authTokenService, userService, shopService);
    }

    @Test
    void sendMessageToShop_success() throws Exception {
        String token = "valid-token";
        int userId = 1;
        int shopId = 2;
        String content = "Hello, shop!";
        int previousMessageId = 0;

        when(authTokenService.ValidateToken("valid-token")).thenReturn(1);
        doNothing().when(userService).validateMemberId(userId);
        when(shopService.getShop(shopId, token)).thenReturn(new Shop(previousMessageId, content, null));
        when(messageRepository.isMessagePrevious(previousMessageId, userId, shopId)).thenReturn(true);

        String result = messageService.sendMessageToShop(token, shopId, content, previousMessageId);
        assertEquals("Message sent successfully!", result);
        verify(messageRepository).addMessage(eq(userId), eq(shopId), eq(content), anyString(), eq(false),
                eq(previousMessageId));
    }

    @Test
    void sendMessageToShop_fails_emptyMessage() throws Exception {
        String token = "valid-token";
        int userId = 1;
        int shopId = 2;
        String content = "";
        int previousMessageId = 0;

        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        doNothing().when(userService).validateMemberId(userId);
        when(shopService.getShop(shopId, token)).thenReturn(new Shop(shopId, content, null));
        when(messageRepository.isMessagePrevious(previousMessageId, userId, shopId)).thenReturn(true);

        doThrow(new OurRuntime("unable to send - message is empty."))
                .when(messageRepository).addMessage(1, 2, "", LocalDate.now().toString(), false, 0);

        String result = messageService.sendMessageToShop(token, shopId, "", previousMessageId);
        assertEquals(result,
                "Error sending message to shop: MosheTheDebugException thrown! mesage: unable to send - message is empty. objects involved: []");
    }

    @Test
    void sendMessageToShop_fails_userNotMember() throws Exception {
        String token = "invalid-token";
        int shopId = 2;
        String content = "Hello!";
        int previousMessageId = 0;

        when(authTokenService.ValidateToken(token)).thenReturn(1);
        doThrow(new OurRuntime("User is not a member")).when(userService).validateMemberId(1);

        String result = messageService.sendMessageToShop(token, shopId, content, previousMessageId);
        assertTrue(result.contains("User is not a member"));
    }

    @Test
    void sendMessageToShop_fails_shopDoesNotExist() throws Exception {
        String token = "valid-token";
        int userId = 1;
        int shopId = 999;
        String content = "Hello!";
        int previousMessageId = 0;

        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        doNothing().when(userService).validateMemberId(userId);
        when(shopService.getShop(shopId, token)).thenReturn(null); // Shop doesn't exist

        String result = messageService.sendMessageToShop(token, shopId, content, previousMessageId);
        assertTrue(result.contains("Shop with ID " + shopId + " doesn't exist."));
    }
}
