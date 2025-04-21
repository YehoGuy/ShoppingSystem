package InfrastructureLayerTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DomainLayer.Item.Item;
import DomainLayer.Item.ItemReview;
import InfrastructureLayer.ItemRepository;

public class ItemRepositoryTests {

    private ItemRepository repo;

    @BeforeEach
    public void setup() {
        repo = new ItemRepository();
    }

    @Test
    public void testCreateAndGetItem() {
        Item item = repo.createItem("Widget", "A useful widget",1);
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
        Item item = repo.createItem("Gadget", "Cool gadget",1);
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
        Item item = repo.createItem("Tool", "Handy tool",1);
        assertEquals(-1.0, repo.getItemAverageRating(item.getId()));
        repo.addReviewToItem(item.getId(), 4, "Good");
        repo.addReviewToItem(item.getId(), 2, "Poor");
        assertEquals(3.0, repo.getItemAverageRating(item.getId()));
    }

    @Test
    public void testDeleteItem() {
        Item item = repo.createItem("DeleteMe", "To be deleted",1);
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
}