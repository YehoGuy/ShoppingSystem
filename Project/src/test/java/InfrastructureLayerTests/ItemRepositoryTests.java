package InfrastructureLayerTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Item.ItemReview;
import com.example.app.InfrastructureLayer.ItemRepository;

public class ItemRepositoryTests {

    private ItemRepository repo;

    @BeforeEach
    public void setup() {
        repo = new ItemRepository();
    }

    @Test
    public void testCreateAndGetItem() {
        Integer itemId = repo.createItem("Widget", "A useful widget",1);
        Item item = repo.getItem(itemId);
        assertNotNull(item);
        assertEquals("Widget", item.getName());
        assertEquals("A useful widget", item.getDescription());
        // fetched same instance
        Item fetched = repo.getItem(item.getId());
        assertSame(item, fetched);
    }

    @Test
    public void testGetAllItems() {
        repo.createItem("A", "Desc A",1);
        repo.createItem("B", "Desc B",1);
        List<Item> all = repo.getAllItems();
        assertEquals(2, all.size());
    }

    @Test
    public void testAddReviewAndGetReviews() {
        Integer itemId = repo.createItem("Gadget", "Cool gadget",1);
        Item item = repo.getItem(itemId);
        assertTrue(repo.getItemReviews(item.getId()).isEmpty());
        repo.addReviewToItem(item.getId(), 5, "Excellent");
        repo.addReviewToItem(item.getId(), 3, "Meh");
        List<ItemReview> reviews = repo.getItemReviews(item.getId());
        assertEquals(2, reviews.size());
        assertEquals(5, reviews.get(0).getRating());
        assertEquals("Excellent", reviews.get(0).getReviewText());
    }

    @Test
    public void testGetAverageRating() {
        Integer itemId = repo.createItem("Tool", "Handy tool",1);
        Item item = repo.getItem(itemId);
        assertEquals(0.0, repo.getItemAverageRating(item.getId()));
        repo.addReviewToItem(item.getId(), 4, "Good");
        repo.addReviewToItem(item.getId(), 2, "Poor");
        assertEquals(3.0, repo.getItemAverageRating(item.getId()));
    }

    @Test
    public void testDeleteItem() {
        Integer itemId = repo.createItem("DeleteMe", "To be deleted",1);
        Item item = repo.getItem(itemId);
        assertNotNull(repo.getItem(item.getId()));
        repo.deleteItem(item.getId());
        assertNull(repo.getItem(item.getId()));
    }

    @Test
    public void testNonexistentItemThrows() {
        int badId = 999;
        assertNull(repo.getItem(badId));
        assertThrows(IllegalArgumentException.class, () -> repo.addReviewToItem(badId, 1, "x"));
        assertThrows(IllegalArgumentException.class, () -> repo.getItemReviews(badId));
        assertThrows(IllegalArgumentException.class, () -> repo.getItemAverageRating(badId));
        assertThrows(IllegalArgumentException.class, () -> repo.deleteItem(badId));
    }

    @Test
    void testCreateGetAll() {
        int id1 = repo.createItem("n1","d1",ItemCategory.ELECTRONICS.ordinal());
        int id2 = repo.createItem("n2","d2",ItemCategory.BOOKS.ordinal());
        assertEquals(id1, repo.getItem(id1).getId());
        List<Item> all = repo.getAllItems();
        assertTrue(all.size() >= 2);
    }

    @Test
    void testReviewAndAverage() {
        int id = repo.createItem("x","y",0);
        repo.addReviewToItem(id,5,"good");
        repo.addReviewToItem(id,3,"ok");
        List<ItemReview> revs = repo.getItemReviews(id);
        assertEquals(2, revs.size());
        assertEquals(4.0, repo.getItemAverageRating(id));
    }

    @Test
    void testReviewInvalidItem_Throws() {
        assertThrows(IllegalArgumentException.class, () -> repo.addReviewToItem(999,5,"r"));
        assertThrows(IllegalArgumentException.class, () -> repo.getItemReviews(999));
        assertThrows(IllegalArgumentException.class, () -> repo.getItemAverageRating(999));
    }

    @Test
    void testDeleteAndGetByIdsAndCategory() {
        int id = repo.createItem("z","w",ItemCategory.CLOTHING.ordinal());
        repo.deleteItem(id);
        assertThrows(IllegalArgumentException.class, () -> repo.deleteItem(id));
        List<Item> some = repo.getItemsByIds(List.of(id, -1));
        assertTrue(some.isEmpty());
        // category
        int e = repo.createItem("e","d",ItemCategory.ELECTRONICS.ordinal());
        List<Integer> elect = repo.getItemsByCategory(ItemCategory.ELECTRONICS);
        assertTrue(elect.contains(e));
        assertTrue(repo.getItemsByCategory(null).isEmpty());
    }
}