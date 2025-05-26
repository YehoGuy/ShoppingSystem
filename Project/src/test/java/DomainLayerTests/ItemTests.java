package DomainLayerTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemReview;

public class ItemTests {

    private Item item;

    @BeforeEach
    public void setup() {
        item = new Item(1, "Widget", "A useful widget",1);
    }

    @Test
    public void testInitialState() {
        assertEquals(1, item.getId());
        assertEquals("Widget", item.getName());
        assertEquals("A useful widget", item.getDescription());
        // No reviews initially
        List<ItemReview> reviews = item.getReviews();
        assertTrue(reviews.isEmpty());
        // Average rating for no reviews is -1.0
        assertEquals(0.0, item.getAverageRating());
    }

    @Test
    public void testAddReviewAndGetReviews() {
        item.addReview(5, "Excellent");
        item.addReview(new ItemReview(3, "Average"));
        List<ItemReview> reviews = item.getReviews();
        assertEquals(2, reviews.size());
        assertEquals(5, reviews.get(0).getRating());
        assertEquals("Excellent", reviews.get(0).getReviewText());
        assertEquals(3, reviews.get(1).getRating());
        assertEquals("Average", reviews.get(1).getReviewText());
    }

    @Test
    public void testAverageRatingCalculation() {
        item.addReview(4, "Good");
        item.addReview(2, "Poor");
        // (4 + 2) / 2 = 3.0
        assertEquals(3.0, item.getAverageRating());
    }

    @Test
    public void testReviewsListUnmodifiable() {
        item.addReview(5, "Great");
        List<ItemReview> reviews = item.getReviews();
        assertThrows(UnsupportedOperationException.class, () -> reviews.add(new ItemReview(1, "Bad")));
    }

    @Test
    public void testToStringContainsFields() {
        item.addReview(4, "Nice");
        String str = item.toString();
        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("name='Widget'"));
        assertTrue(str.contains("description='A useful widget'"));
        assertTrue(str.contains("averageRating=4.0"));
        assertTrue(str.contains("ItemReview"));
    }
}