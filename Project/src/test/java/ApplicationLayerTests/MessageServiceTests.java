package ApplicationLayerTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.Message.MessageService;
import ApplicationLayer.Purchase.ShippingMethod;
import ApplicationLayer.Shop.ShopService;
import ApplicationLayer.User.UserService;
import DomainLayer.Shop.PurchasePolicy;
import InfrastructureLayer.AuthTokenRepository;
import InfrastructureLayer.ItemRepository;
import InfrastructureLayer.MessageRepository;
import InfrastructureLayer.ShopRepository;
import InfrastructureLayer.UserRepository;


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
        messageService = new MessageService(new MessageRepository());
        authTokenRepository = new AuthTokenRepository();
        authTokenService = new AuthTokenService(authTokenRepository);
        userRepository = new UserRepository();
        userService = new UserService(userRepository);
        shopRepository = new ShopRepository();
        shopService = new ShopService(shopRepository);
    
        messageService.setService(authTokenService, userService, shopService); // Set the services for the message service);
        shopService.setServices(authTokenService, new ItemService(new ItemRepository()), userService); // Set the user service for the shop service

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
        assertEquals("Error sending message to user: User with ID 9999 doesn't exist.", messageService.sendMessageToUser(token1, 9999, "Hello", 0));
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
        assertEquals("Error sending message to shop: Error retrieving shop with id 9999: Error retrieving shop: Shop not found: 9999", messageService.sendMessageToShop(token1, 9999, "Hello", 0));
    }

}
