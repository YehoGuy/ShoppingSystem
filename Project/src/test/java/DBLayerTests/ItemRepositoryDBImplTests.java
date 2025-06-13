package DBLayerTests;

import com.example.app.SimpleHttpServerApplication;
import com.example.app.ApplicationLayer.OurRuntime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.app.DBLayer.Item.ItemRepositoryDBImpl;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Item.ItemReview;

import jakarta.transaction.Transactional;

@SpringBootTest(classes = SimpleHttpServerApplication.class)
@ActiveProfiles({ "test" })
@Transactional
public class ItemRepositoryDBImplTests {

    @Autowired
    private ItemRepositoryDBImpl repo;

    @Test
    public void testCreateAndGetItem() {
        Integer itemId = repo.createItem("Widget", "A useful widget", 1);
        Item item = repo.getItem(itemId);
        assertNotNull(item);
        assertEquals("Widget", item.getName());
        assertEquals("A useful widget", item.getDescription());
    }

    @Test
    public void testGetAllItems() {
        repo.createItem("A", "Desc A", 1);
        repo.createItem("B", "Desc B", 1);
        List<Item> all = repo.getAllItems();
        assertEquals(2, all.size());
    }

    @Test
    public void testAddReviewAndGetReviews() {
        int itemId = repo.createItem("Gadget", "Cool gadget", 1);
        assertTrue(repo.getItemReviews(itemId).isEmpty());
        repo.addReviewToItem(itemId, 5, "Excellent");
        repo.addReviewToItem(itemId, 3, "Meh");
        List<ItemReview> reviews = repo.getItemReviews(itemId);
        assertEquals(2, reviews.size());
        assertEquals(5, reviews.get(0).getRating());
        assertEquals("Excellent", reviews.get(0).getReviewText());
    }

    @Test
    public void testGetAverageRating() {
        int id = repo.createItem("Tool", "Handy tool", 1);
        assertEquals(0.0, repo.getItemAverageRating(id));
        repo.addReviewToItem(id, 4, "Good");
        repo.addReviewToItem(id, 2, "Bad");
        assertEquals(3.0, repo.getItemAverageRating(id));
    }

    @Test
    public void testDeleteItem() {
        int id = repo.createItem("ToDelete", "x", 1);
        assertNotNull(repo.getItem(id));
        repo.deleteItem(id);
        assertThrows(OurRuntime.class, () -> repo.getItem(id));
    }

    @Test
    public void testNonexistentItemThrows() {
        assertThrows(OurRuntime.class, () -> repo.getItem(999));
        assertThrows(OurRuntime.class, () -> repo.addReviewToItem(999, 5, "bad"));
        assertThrows(OurRuntime.class, () -> repo.getItemReviews(999));
        assertThrows(OurRuntime.class, () -> repo.getItemAverageRating(999));
    }

    @Test
    public void testCreateGetAll() {
        int id1 = repo.createItem("n1", "d1", ItemCategory.ELECTRONICS.ordinal());
        int id2 = repo.createItem("n2", "d2", ItemCategory.BOOKS.ordinal());
        assertEquals(id1, repo.getItem(id1).getId());
        List<Item> all = repo.getAllItems();
        assertTrue(all.size() >= 2);
    }

    @Test
    public void testReviewAndAverage() {
        int id = repo.createItem("x", "y", 0);
        repo.addReviewToItem(id, 5, "great");
        repo.addReviewToItem(id, 3, "ok");
        List<ItemReview> revs = repo.getItemReviews(id);
        assertEquals(2, revs.size());
        assertEquals(4.0, repo.getItemAverageRating(id));
    }

    @Test
    public void testDeleteAndGetByIdsAndCategory() {
        int id = repo.createItem("z", "w", ItemCategory.CLOTHING.ordinal());
        repo.deleteItem(id);
        List<Item> some = repo.getItemsByIds(List.of(id, -1));
        assertTrue(some.isEmpty());

        int e = repo.createItem("e", "d", ItemCategory.ELECTRONICS.ordinal());
        List<Integer> elect = repo.getItemsByCategory(ItemCategory.ELECTRONICS);
        assertTrue(elect.contains(e));
        assertTrue(repo.getItemsByCategory(null).isEmpty());
    }

}
