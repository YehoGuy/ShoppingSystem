package ApplicationLayerTests.Item;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.User.UserService;
import DomainLayer.Item.IItemRepository;
import DomainLayer.Item.Item;
import DomainLayer.Roles.PermissionsEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ItemServiceAcceptanceTests {

    private IItemRepository itemRepository;
    private AuthTokenService authTokenService;
    private UserService userService;
    private ItemService itemService;

    private final int SHOP_ID = 42;
    private final String TOKEN = "validToken";
    private final int USER_ID = 7;

    @BeforeEach
    public void setUp() throws Exception {
        itemRepository    = mock(IItemRepository.class);
        authTokenService  = mock(AuthTokenService.class);
        userService       = mock(UserService.class);

        itemService = new ItemService(itemRepository);
        itemService.setServices(authTokenService, userService);

        // common stub: token validation returns USER_ID
        doReturn(USER_ID).when(authTokenService).ValidateToken(TOKEN);
    }

    // UC5 – Create Item (positive)
    @Test
    public void testCreateItemSuccess() throws Exception {
        String name        = "Widget";
        String description = "A test widget";
        Integer category   = 0;
        Integer newItemId  = 123;

        // user has permission
        when(userService.hasPermission(USER_ID, PermissionsEnum.manageItems, SHOP_ID))
            .thenReturn(true);

        // repository creates item
        when(itemRepository.createItem(name, description, category))
            .thenReturn(newItemId);

        Integer result = itemService.createItem(
            SHOP_ID, name, description, category, TOKEN
        );

        assertEquals(newItemId, result);
        verify(itemRepository).createItem(name, description, category);
    }

    // UC5 – Create Item (negative: repo failure)
    @Test
    public void testCreateItemInvalid() {
        String name      = "";
        String description = "desc";
        Integer category = 0;

        when(userService.hasPermission(USER_ID, PermissionsEnum.manageItems, SHOP_ID))
            .thenReturn(true);

        // repository throws
        when(itemRepository.createItem(name, description, category))
            .thenThrow(new RuntimeException("Database error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            itemService.createItem(SHOP_ID, name, description, category, TOKEN)
        );
        assertTrue(ex.getMessage().contains("Error creating item"));
    }

    // UC11 – Add Review to Item (positive)
    @Test
    public void testAddReviewToItemSuccess() {
        int itemId   = 5;
        int rating   = 4;
        String text  = "Good item";

        // repository accepts review
        doNothing().when(itemRepository)
            .addReviewToItem(itemId, rating, text);

        assertDoesNotThrow(() ->
            itemService.addReviewToItem(itemId, rating, text, TOKEN)
        );
        verify(itemRepository).addReviewToItem(itemId, rating, text);
    }

    // UC11 – Add Review to Item (negative: invalid)
    @Test
    public void testAddReviewToItemInvalid() {
        int itemId  = 5;
        int rating  = 0;      // invalid
        String text = "";     // invalid

        doThrow(new RuntimeException("Invalid review"))
            .when(itemRepository)
            .addReviewToItem(itemId, rating, text);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            itemService.addReviewToItem(itemId, rating, text, TOKEN)
        );
        assertTrue(ex.getMessage().contains("Error adding review to item"));
    }
    

    // Optional: Fetch item by ID
    @Test
    public void testGetItemSuccess() {
        int itemId = 7;
        Item item  = new Item(itemId, "Name", "Desc", 1);

        when(itemRepository.getItem(itemId)).thenReturn(item);

        Item fetched = itemService.getItem(itemId, TOKEN);
        assertEquals(item, fetched);
    }

    // Optional: Fetch all items
    @Test
    public void testGetAllItemsSuccess() {
        List<Item> mockList = Arrays.asList(
            new Item(1,"A","D",0),
            new Item(2,"B","D2",1)
        );
        when(itemRepository.getAllItems()).thenReturn(mockList);

        List<Item> all = itemService.getAllItems(TOKEN);
        assertEquals(mockList, all);
    }
}
