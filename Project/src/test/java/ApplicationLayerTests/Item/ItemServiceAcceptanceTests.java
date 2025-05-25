package ApplicationLayerTests.Item;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.Item.ItemService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Item.IItemRepository;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Item.ItemReview;
import com.example.app.DomainLayer.Roles.PermissionsEnum;


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

        itemService = new ItemService(itemRepository,authTokenService, userService);

        // common stub: token validation returns USER_ID
        doReturn(USER_ID).when(authTokenService).ValidateToken(TOKEN);
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
    
        // UC11 – Add Review to Item (negative: invalid rating)
        @Test
        public void testAddReviewToItemInvalidRating() {
            int itemId  = 5;
            int rating  = 0;      // invalid
            String text = "abc";
    
            doThrow(new RuntimeException("Invalid review"))
                .when(itemRepository)
                .addReviewToItem(itemId, rating, text);
    
            assertThrows(RuntimeException.class, () ->
                itemService.addReviewToItem(itemId, rating, text, TOKEN)
            );
        }

        // UC11 – Add Review to Item (negative: invaklid description)
        @Test
        public void testAddReviewToItemInvalidDescription() {
            int itemId  = 5;
            int rating  = 4;
            String text = ""; // invalid
    
            doThrow(new RuntimeException("Invalid review"))
                .when(itemRepository)
                .addReviewToItem(itemId, rating, text);
    
            assertThrows(RuntimeException.class, () ->
                itemService.addReviewToItem(itemId, rating, text, TOKEN)
            );
        }

    // UC16 – Create Item (positive)
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

    // UC16 – Create Item (invalid no permission)
    @Test
    public void testCreateItemNoPermission() {
        String name        = "Widget";
        String description = "A test widget";
        Integer category   = 0;

        // user does not have permission
        when(userService.hasPermission(USER_ID, PermissionsEnum.manageItems, SHOP_ID))
            .thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            itemService.createItem(SHOP_ID, name, description, category, TOKEN)
        );
        assertTrue(ex.getMessage().contains("User does not have permission to add item to shop"));
    }

    // UC16 – Create Item (invalid name)
    @Test
    public void testCreateItemInvalidName() {
        String name      = "";
        String description = "desc";
        Integer category = 1;

        when(userService.hasPermission(USER_ID, PermissionsEnum.manageItems, SHOP_ID))
            .thenReturn(true);

        // repository throws
        when(itemRepository.createItem(name, description, category))
            .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () ->
            itemService.createItem(SHOP_ID, name, description, category, TOKEN)
        );

    }

    // UC16 – Create Item (invalid description)
    @Test
    public void testCreateItemInvalidDescription() {
        String name        = "Widget";
        String description = ""; // invalid
        Integer category   = 0;

        when(userService.hasPermission(USER_ID, PermissionsEnum.manageItems, SHOP_ID))
            .thenReturn(true);

        // repository throws
        when(itemRepository.createItem(name, description, category))
            .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () ->
            itemService.createItem(SHOP_ID, name, description, category, TOKEN)
        );
    }

    // UC16 – Create Item (invalid category)
    @Test
    public void testCreateItemInvalidCategory() {
        String name        = "Widget";
        String description = "A test widget";
        Integer category   = -1; // invalid

        when(userService.hasPermission(USER_ID, PermissionsEnum.manageItems, SHOP_ID))
            .thenReturn(true);

        assertThrows(RuntimeException.class, () ->
            itemService.createItem(SHOP_ID, name, description, category, TOKEN)
        );
    }
       

    // UC16 – Create Item (invalid shop ID)
    @Test
    public void testCreateItemInvalidShopId() {
        String name        = "Widget";
        String description = "A test widget";
        Integer category   = 0;
        Integer shopId     = -1; // invalid

        when(userService.hasPermission(USER_ID, PermissionsEnum.manageItems, shopId))
            .thenReturn(true);

            assertThrows(RuntimeException.class, () ->
            itemService.createItem(shopId, name, description, category, TOKEN)
        );
    }

    // Uc5 - search item in the market (positive)
    @Test
    public void testGetItemSuccess() {
        int itemId = 7;
        Item item  = new Item(itemId, "Name", "Desc", 1);

        when(itemRepository.getItem(itemId)).thenReturn(item);

        Item fetched = itemService.getItem(itemId, TOKEN);
        assertEquals(item, fetched);
    }

    // Uc5 - search item in the market (negative: invalid item ID)
    @Test
    public void testGetItemInvalidId() {
        int itemId = -1; // invalid

        RuntimeException ex = assertThrows(OurArg.class, () ->
            itemService.getItem(itemId, TOKEN)
        );
        assertTrue(ex.getMessage().contains("Item ID cannot be negative"));
    }

    // Uc5 - search item in the market (negative: item not found)
    @Test
    public void testGetItemNotFound() {
        int itemId = 7;

        // repository returns null
        when(itemRepository.getItem(itemId)).thenReturn(null);

        assertNull(itemService.getItem(itemId, TOKEN));
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

    // ─── getItemsByIds ───────────────────────────────────────────────────────
    @Test
    public void testGetItemsByIdsSuccess() {
        List<Integer> ids = Arrays.asList(1, 2);
        Item i1 = new Item(1, "A", "D", 0);
        Item i2 = new Item(2, "B", "E", 1);

        when(itemRepository.getItemsByIds(ids)).thenReturn(Arrays.asList(i1, i2));

        List<Item> result = itemService.getItemsByIds(ids, TOKEN);
        assertEquals(Arrays.asList(i1, i2), result);
    }

    @Test
    public void testGetItemsByIdsInvalidNullOrEmpty() {
        // null list → OurArg
        assertThrows(OurArg.class,
            () -> itemService.getItemsByIds(null, TOKEN),
            "null list should throw");

        // empty list → OurArg
        assertEquals(itemService.getItemsByIds(List.of(), TOKEN).size(), 0,
            "empty list should return empty result");

    }

    @Test
    public void testGetItemsByIdsNegativeId() {
        assertThrows(OurArg.class,
            () -> itemService.getItemsByIds(Arrays.asList(1, -2), TOKEN),
            "negative ID in list should throw");
    }

    @Test
    public void testGetItemsByIdsAuthError() throws Exception {
        List<Integer> ids = List.of(1);
        doThrow(new OurRuntime("bad token"))
            .when(authTokenService).ValidateToken("bad");
        assertThrows(OurRuntime.class,
            () -> itemService.getItemsByIds(ids, "bad"),
            "auth failure should throw");
    }


    // ─── getItemReviews ──────────────────────────────────────────────────────
    @Test
    public void testGetItemReviewsSuccess2() {
        int itemId = 3;
        // Mockito mock instead of calling a nonexistent constructor
        ItemReview r1 = mock(ItemReview.class);
        when(itemRepository.getItemReviews(itemId)).thenReturn(List.of(r1));

        List<ItemReview> reviews = itemService.getItemReviews(itemId, TOKEN);
        assertEquals(1, reviews.size(), "should return exactly one review");
        assertSame(r1, reviews.get(0), "should return the same review instance");
    }

    @Test
    public void testGetItemReviewsInvalidId2() {
        assertThrows(OurArg.class,
            () -> itemService.getItemReviews(-1, TOKEN),
            "negative itemId should throw");
    }

    @Test
    public void testGetItemReviewsAuthError() throws Exception {
        doThrow(new OurRuntime("nope"))
            .when(authTokenService).ValidateToken("bad");
        assertThrows(OurRuntime.class,
            () -> itemService.getItemReviews(1, "bad"),
            "auth error should throw");
    }


    // ─── getItemAverageRating ────────────────────────────────────────────────
    @Test
    public void testGetItemAverageRatingSuccess2() {
        when(itemRepository.getItemAverageRating(4)).thenReturn(3.14);
        double avg = itemService.getItemAverageRating(4, TOKEN);
        assertEquals(3.14, avg, 1e-6);
    }

    @Test
    public void testGetItemAverageRatingInvalidId2() {
        assertThrows(OurArg.class,
            () -> itemService.getItemAverageRating(-1, TOKEN),
            "negative itemId should throw");
    }

    @Test
    public void testGetItemAverageRatingAuthError() throws Exception {
        when(itemRepository.getItemAverageRating(4)).thenReturn(1.0);
        doThrow(new OurRuntime("bad token"))
            .when(authTokenService).ValidateToken("bad");
        assertThrows(OurRuntime.class,
            () -> itemService.getItemAverageRating(4, "bad"),
            "auth failure should throw");
    }


    // ─── getItemsByCategory ─────────────────────────────────────────────────
    @Test
    public void testGetItemsByCategorySuccess2() {
        List<Integer> catList = List.of(10, 20);
        when(itemRepository.getItemsByCategory(ItemCategory.ELECTRONICS))
            .thenReturn(catList);

        List<Integer> result =
            itemService.getItemsByCategory(ItemCategory.ELECTRONICS, TOKEN);
        assertEquals(catList, result);
    }

    @Test
    public void testGetItemsByCategoryNull() {
        assertThrows(OurArg.class,
            () -> itemService.getItemsByCategory(null, TOKEN),
            "null category should throw");
    }

    @Test
    public void testGetItemsByCategoryAuthError() throws Exception {
        doThrow(new OurRuntime("nope"))
            .when(authTokenService).ValidateToken("bad");
        assertThrows(OurRuntime.class,
            () -> itemService.getItemsByCategory(ItemCategory.ELECTRONICS, "bad"),
            "auth failure should throw");
    }


    // ─── getItemdId2Cat ──────────────────────────────────────────────────────
    @Test
    public void testGetItemdId2CatMix() {
        Map<Integer,Integer> map = new HashMap<>();
        map.put(1, 5);
        map.put(2, 10);

        Item i1 = new Item(1, "X", "D", ItemCategory.ELECTRONICS.ordinal());
        when(itemRepository.getItem(1)).thenReturn(i1);
        when(itemRepository.getItem(2)).thenReturn(null);

        Map<Integer,ItemCategory> result = itemService.getItemdId2Cat(map);
        assertEquals(1, result.size());
        assertEquals(i1.getCategory(), result.get(1));
    }

    @Test
    public void testGetItemdId2CatEmpty() {
        Map<Integer,Integer> empty = Map.of();
        Map<Integer,ItemCategory> result = itemService.getItemdId2Cat(empty);
        assertTrue(result.isEmpty(), "Empty input map should produce an empty category map");
    }



    // ─── getAllItems exception branches ──────────────────────────────────────
    @Test
    public void testGetAllItemsAuthError() throws Exception {
        doThrow(new OurRuntime("no auth"))
            .when(authTokenService).ValidateToken("bad");
        assertThrows(OurRuntime.class,
            () -> itemService.getAllItems("bad"),
            "auth failure should throw");
    }

    @Test
    public void testGetAllItemsRepositoryError() {
        when(itemRepository.getAllItems())
            .thenThrow(new RuntimeException("db error"));
        assertThrows(RuntimeException.class,
            () -> itemService.getAllItems(TOKEN),
            "repo exception should bubble");
    }

    // ─── getAllItems ─────────────────────────────────────────────────────────────
    @Test
    public void testGetAllItemsTokenInvalid() throws Exception {
        // simulate token validation failure
        doThrow(new OurRuntime("Invalid token"))
            .when(authTokenService).ValidateToken("badToken");

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> itemService.getAllItems("badToken")
        );
        assertTrue(ex.getMessage().contains("getAllItems"));
    }

    @Test
    public void testGetAllItemsRepoThrows() throws Exception {
        when(authTokenService.ValidateToken(TOKEN)).thenReturn(USER_ID);
        doThrow(new RuntimeException("db error"))
            .when(itemRepository).getAllItems();

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> itemService.getAllItems(TOKEN)
        );
        assertTrue(ex.getMessage().contains("getAllItems: db error"));
    }

    // ─── getItemAverageRating ────────────────────────────────────────────────────
    @Test
    public void testGetItemAverageRatingSuccess() {
        int itemId = 8;
        when(itemRepository.getItemAverageRating(itemId)).thenReturn(4.5);
        double avg = itemService.getItemAverageRating(itemId, TOKEN);
        assertEquals(4.5, avg);
    }

    @Test
    public void testGetItemAverageRatingNoReviews() {
        int itemId = 9;
        when(itemRepository.getItemAverageRating(itemId)).thenReturn(-1.0);
        double avg = itemService.getItemAverageRating(itemId, TOKEN);
        assertEquals(-1.0, avg);
    }

    @Test
    public void testGetItemAverageRatingInvalidId() {
        assertThrows(OurArg.class,
            () -> itemService.getItemAverageRating(-1, TOKEN)
        );
    }

    @Test
    public void testGetItemAverageRatingRepoThrows() {
        int itemId = 10;
        when(itemRepository.getItemAverageRating(itemId))
            .thenThrow(new RuntimeException("rte"));

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> itemService.getItemAverageRating(itemId, TOKEN)
        );
        assertTrue(ex.getMessage().contains("getItemAverageRating " + itemId + ": rte"));
    }

    // ─── getItemsByCategory ──────────────────────────────────────────────────────
    @Test
    public void testGetItemsByCategorySuccess() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        when(itemRepository.getItemsByCategory(ItemCategory.ELECTRONICS)).thenReturn(list);

        List<Integer> result = itemService.getItemsByCategory(ItemCategory.ELECTRONICS, TOKEN);
        assertEquals(list, result);
    }

    @Test
    public void testGetItemsByCategoryNullCategory() {
        assertThrows(OurArg.class,
            () -> itemService.getItemsByCategory(null, TOKEN)
        );
    }

    @Test
    public void testGetItemsByCategoryRepoThrows() {
        when(itemRepository.getItemsByCategory(ItemCategory.TOYS))
            .thenThrow(new RuntimeException("fail"));

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> itemService.getItemsByCategory(ItemCategory.TOYS, TOKEN)
        );
        assertTrue(ex.getMessage().contains("getItemsByCategory " + ItemCategory.TOYS + ": fail"));
    }

    // ─── getItemReviews ──────────────────────────────────────────────────────────
    @Test
    public void testGetItemReviewsInvalidId() {
        assertThrows(OurArg.class,
            () -> itemService.getItemReviews(-5, TOKEN)
        );
    }

    @Test
    public void testGetItemReviewsSuccess() {
        int itemId = 5;
        ItemReview mockReview = mock(ItemReview.class);
        when(itemRepository.getItemReviews(itemId)).thenReturn(Arrays.asList(mockReview));

        List<ItemReview> result = itemService.getItemReviews(itemId, TOKEN);
        assertEquals(1, result.size());
        assertSame(mockReview, result.get(0));
    }

    @Test
    public void testGetItemReviewsRepoThrows() {
        int itemId = 6;
        when(itemRepository.getItemReviews(itemId))
            .thenThrow(new RuntimeException("err"));

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> itemService.getItemReviews(itemId, TOKEN)
        );
        assertTrue(ex.getMessage().contains("getItemReviews " + itemId + ": err"));
    }

    // ─── addReviewToItem ─────────────────────────────────────────────────────────
    @Test
    public void testAddReviewToItemInvalidId() {
        assertThrows(OurArg.class,
            () -> itemService.addReviewToItem(-1, 4, "text", TOKEN)
        );
    }

    @Test
    public void testAddReviewToItemInvalidRatingLow() {
        assertThrows(OurArg.class,
            () -> itemService.addReviewToItem(1, 0, "text", TOKEN)
        );
    }

    @Test
    public void testAddReviewToItemInvalidRatingHigh() {
        assertThrows(OurArg.class,
            () -> itemService.addReviewToItem(1, 6, "text", TOKEN)
        );
    }

    @Test
    public void testAddReviewToItemInvalidText() {
        assertThrows(OurArg.class,
            () -> itemService.addReviewToItem(1, 4, "", TOKEN)
        );
    }

    @Test
    public void testAddReviewToItemRepoThrows() {
        int itemId = 3;
        doThrow(new RuntimeException("dbfail"))
            .when(itemRepository).addReviewToItem(itemId, 5, "good");

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> itemService.addReviewToItem(itemId, 5, "good", TOKEN)
        );
        assertTrue(ex.getMessage().contains("addReviewToItem " + itemId + ": dbfail"));
    }

    // ─── getItem ─────────────────────────────────────────────────────────────────
    @Test
    public void testGetItemRepoThrows() {
        int itemId = 9;
        when(itemRepository.getItem(itemId))
            .thenThrow(new RuntimeException("oops"));

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> itemService.getItem(itemId, TOKEN)
        );
        assertTrue(ex.getMessage().contains("getItem " + itemId + ": oops"));
    }

    // ─── getItemdId2Cat ──────────────────────────────────────────────────────────
    @Test
    public void testGetItemdId2CatWithEntries() {
        Map<Integer,Integer> input = Map.of(1,10, 2,20);
        Item i1 = new Item(1,"n","d",0);
        Item i2 = new Item(2,"m","desc",2);
        when(itemRepository.getItem(1)).thenReturn(i1);
        when(itemRepository.getItem(2)).thenReturn(i2);

        Map<Integer,ItemCategory> result = itemService.getItemdId2Cat(input);
        assertEquals(2, result.size());
        assertEquals(i1.getCategory(), result.get(1));
        assertEquals(i2.getCategory(), result.get(2));
    }

    @Test
    public void testGetItemdId2CatSkipsNull() {
        Map<Integer,Integer> input = Map.of(3,5);
        when(itemRepository.getItem(3)).thenReturn(null);

        Map<Integer,ItemCategory> result = itemService.getItemdId2Cat(input);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetItemdId2CatRepoThrows() {
        Map<Integer,Integer> input = Map.of(4,1);
        when(itemRepository.getItem(4))
            .thenThrow(new RuntimeException("fail"));

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> itemService.getItemdId2Cat(input)
        );
        assertTrue(ex.getMessage().contains("getItemdId2Cat: fail"));
    }

}