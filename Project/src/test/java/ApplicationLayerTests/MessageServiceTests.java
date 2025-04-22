package ApplicationLayerTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.Message.MessageService;
import ApplicationLayer.Shop.ShopService;
import ApplicationLayer.User.UserService;
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
    private String token2;
    
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
        userRepository.addMember("testUser", "password", "a", "b", "c");
        userRepository.addMember("testUser2", "password", "a", "b", "c");
        token2 = authTokenService.Login("testUser2", "password2", 2);
        token1 = authTokenService.Login("testUser", "password", 1);
        shopService.createShop("shop1", "b", 2, token1);
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
        assertEquals("Error sending message to user: Previous message with ID 9999 isn't proper previous message.", messageService.sendMessageToUser(token1, 2, "Hello", 9999));
    }

    @Test
    void testSendMessageToShopWithInvalidToken() {
        // Test sending a message to a shop with an invalid token
        assertEquals("Error sending message to shop: Invalid token", messageService.sendMessageToShop("invalidToken", 1, "Hello", 0));
    }

    @Test
    void testSendMessageToShopWithInvalidReceiverId() {
        // Test sending a message to an invalid receiver ID
        assertEquals("Error sending message to shop: Shop with ID 9999 doesn't exist.", messageService.sendMessageToShop(token1, 9999, "Hello", 0));
    }

}
