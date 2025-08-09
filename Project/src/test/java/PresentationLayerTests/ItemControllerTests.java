package PresentationLayerTests;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Item.ItemService;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Item.ItemReview;
import com.example.app.PresentationLayer.Controller.ItemController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

/**
 * Comprehensive slice tests for ItemController.
 */
@WebMvcTest(controllers = ItemController.class)
@ContextConfiguration(classes = ItemControllerTests.TestBootApp.class)
@AutoConfigureMockMvc(addFilters = false)
public class ItemControllerTests {

    @SpringBootApplication(scanBasePackages = "com.example.app.PresentationLayer")
    static class TestBootApp {
    }

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ItemService itemService;

    @Nested
    @DisplayName("1. CREATE ITEM")
    class CreateItem {
        @Test
        void success_returnsCreated() throws Exception {
            when(itemService.createItem(1, "ItemX", "Desc", ItemCategory.ELECTRONICS, "tok"))
                    .thenReturn(123);

            mvc.perform(post("/api/items/create")
                    .param("shopId", "1")
                    .param("name", "ItemX")
                    .param("description", "Desc")
                    .param("category", "ELECTRONICS")
                    .param("token", "tok"))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("123"));
        }

        @Test
        void badRequest_returns400() throws Exception {
            when(itemService.createItem(anyInt(), anyString(), anyString(), eq(ItemCategory.ELECTRONICS), anyString()))
                    .thenThrow(new OurArg("bad"));

            mvc.perform(post("/api/items/create")
                    .param("shopId", "1")
                    .param("name", "")
                    .param("description", "Desc")
                    .param("category", "ELECTRONICS")
                    .param("token", "tok"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_returns409() throws Exception {
            when(itemService.createItem(anyInt(), anyString(), anyString(), eq(ItemCategory.ELECTRONICS), anyString()))
                    .thenThrow(new OurRuntime("conflict"));

            mvc.perform(post("/api/items/create")
                    .param("shopId", "1")
                    .param("name", "ItemX")
                    .param("description", "Desc")
                    .param("category", "ELECTRONICS")
                    .param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("2. GET ITEM")
    class GetItem {
        @Test
        void success_returns200AndItem() throws Exception {
            Item it = new Item(5, "Name", "Desc", 1);
            when(itemService.getItem(5, "tok")).thenReturn(it);

            mvc.perform(get("/api/items/5").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.name").value("Name"));
        }

        @Test
        void badRequest_returns400() throws Exception {
            when(itemService.getItem(anyInt(), anyString()))
                    .thenThrow(new IllegalArgumentException("bad"));

            mvc.perform(get("/api/items/5").param("token", "tok"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_returns409() throws Exception {
            when(itemService.getItem(anyInt(), anyString()))
                    .thenThrow(new OurRuntime("conflict"));

            mvc.perform(get("/api/items/5").param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    @DisplayName("3. GET ALL ITEMS")
    void getAll_returnsList() throws Exception {
        Item a = new Item(1, "A", "D", 0);
        Item b = new Item(2, "B", "E", 1);
        when(itemService.getAllItems("tok")).thenReturn(Arrays.asList(a, b));

        mvc.perform(get("/api/items/all").param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Nested
    @DisplayName("4. ADD REVIEW")
    class AddReview {
        @Test
        void success_returnsAccepted() throws Exception {
            doNothing().when(itemService).addReviewToItem(7, 5, "Good", "tok");

            mvc.perform(post("/api/items/7/reviews")
                    .param("rating", "5")
                    .param("reviewText", "Good")
                    .param("token", "tok"))
                    .andExpect(status().isAccepted());
        }

        @Test
        void badRequest_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(itemService).addReviewToItem(anyInt(), anyInt(), anyString(),
                    anyString());

            mvc.perform(post("/api/items/7/reviews")
                    .param("rating", "0")
                    .param("reviewText", "Good")
                    .param("token", "tok"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException()).when(itemService).addReviewToItem(anyInt(), anyInt(), anyString(),
                    anyString());

            mvc.perform(post("/api/items/7/reviews")
                    .param("rating", "5")
                    .param("reviewText", "Good")
                    .param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("5. LIST REVIEWS")
    class ListReviews {
        @Test
        void success_returnsReviews() throws Exception {
            ItemReview r = new ItemReview(4, "Nice");
            when(itemService.getItemReviews(3, "tok")).thenReturn(Collections.singletonList(r));

            mvc.perform(get("/api/items/3/reviews").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].rating").value(4));
        }

        @Test
        void badRequest_returns400() throws Exception {
            when(itemService.getItemReviews(anyInt(), anyString()))
                    .thenThrow(new IllegalArgumentException());

            mvc.perform(get("/api/items/3/reviews").param("token", "tok"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_returns409() throws Exception {
            when(itemService.getItemReviews(anyInt(), anyString()))
                    .thenThrow(new RuntimeException());

            mvc.perform(get("/api/items/3/reviews").param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("6. GET RATING")
    class GetRating {
        @Test
        void success_returnsOk() throws Exception {
            when(itemService.getItemAverageRating(8, "tok")).thenReturn(4.5);

            mvc.perform(get("/api/items/8/rating").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("4.5"));
        }

        @Test
        void badRequest_returns400() throws Exception {
            when(itemService.getItemAverageRating(anyInt(), anyString()))
                    .thenThrow(new IllegalArgumentException());

            mvc.perform(get("/api/items/8/rating").param("token", "tok"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_returns409() throws Exception {
            when(itemService.getItemAverageRating(anyInt(), anyString()))
                    .thenThrow(new RuntimeException());

            mvc.perform(get("/api/items/8/rating").param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("7. GET BY IDS")
    class GetByIds {
        @Test
        void success_returnsItems() throws Exception {
            Item x = new Item(9, "X", "D", 0);
            when(itemService.getItemsByIds(eq(Arrays.asList(9, 10)), eq("tok")))
                    .thenReturn(Arrays.asList(x));

            mvc.perform(post("/api/items/by-ids")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[9,10]")
                    .param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(9));
        }

        @Test
        void badRequest_returns400() throws Exception {
            when(itemService.getItemsByIds(anyList(), anyString()))
                    .thenThrow(new IllegalArgumentException());

            mvc.perform(post("/api/items/by-ids")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[]")
                    .param("token", "tok"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_returns409() throws Exception {
            when(itemService.getItemsByIds(anyList(), anyString()))
                    .thenThrow(new RuntimeException());

            mvc.perform(post("/api/items/by-ids")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[1]")
                    .param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("8. GET BY CATEGORY")
    class GetByCategory {
        @Test
        void success_returnsIds() throws Exception {
            when(itemService.getItemsByCategory(eq(ItemCategory.BOOKS), eq("tok")))
                    .thenReturn(Arrays.asList(2, 3));

            mvc.perform(get("/api/items/category")
                    .param("category", "BOOKS")
                    .param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value(2));
        }

        @Test
        void badRequest_returns400() throws Exception {
            when(itemService.getItemsByCategory(any(), anyString()))
                    .thenThrow(new IllegalArgumentException());

            mvc.perform(get("/api/items/category")
                    .param("category", "BOOKS")
                    .param("token", "tok"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_returns409() throws Exception {
            when(itemService.getItemsByCategory(any(), anyString()))
                    .thenThrow(new RuntimeException());

            mvc.perform(get("/api/items/category")
                    .param("category", "BOOKS")
                    .param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

        @Nested
        @DisplayName("9. DELETE ITEM")
        class DeleteItem {
                @Test
                void success_returns204() throws Exception {
                doNothing().when(itemService).deleteItem(1, "tok");
                mvc.perform(delete("/api/items/1")
                        .param("token", "tok"))
                .andExpect(status().isNoContent());
                }

                @Test
                void badRequest_returns400() throws Exception {
                doThrow(new IllegalArgumentException("bad id"))
                        .when(itemService).deleteItem(anyInt(), anyString());
                mvc.perform(delete("/api/items/1")
                        .param("token", "tok"))
                .andExpect(status().isBadRequest());
                }

                @Test
                void conflict_returns409_onOurRuntime() throws Exception {
                doThrow(new OurRuntime("conflict"))
                        .when(itemService).deleteItem(anyInt(), anyString());
                mvc.perform(delete("/api/items/1")
                        .param("token", "tok"))
                .andExpect(status().isConflict());
                }

                @Test
                void serverError_returns500() throws Exception {
                // throw an unchecked exception so Mockito is happy
                doThrow(new RuntimeException("boom"))
                        .when(itemService).deleteItem(anyInt(), anyString());

                mvc.perform(delete("/api/items/1")
                        .param("token", "tok"))
                .andExpect(status().isInternalServerError());
                }
        }

        @Nested
        @DisplayName("10. ERROR SCENARIOS FOR OTHER ENDPOINTS")
        class ErrorScenarios {
                @Test @DisplayName("CREATE → RuntimeException → 409")
                void create_runtimeError_returns409() throws Exception {
                when(itemService.createItem(anyInt(), anyString(), anyString(),
                        eq(ItemCategory.ELECTRONICS), anyString()))
                        .thenThrow(new RuntimeException("oops"));

                mvc.perform(post("/api/items/create")
                                .param("shopId",      "1")
                                .param("name",        "X")
                                .param("description", "D")
                                .param("category",    "ELECTRONICS")
                                .param("token",       "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string("oops"));
                }

                @Test @DisplayName("GET ONE → RuntimeException → 409")
                void getOne_runtimeError_returns409() throws Exception {
                when(itemService.getItem(anyInt(), anyString()))
                        .thenThrow(new RuntimeException("err"));

                mvc.perform(get("/api/items/5")
                                .param("token", "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string("err"));
                }

                @Test @DisplayName("GET ALL → RuntimeException → 409")
                void getAll_runtimeError_returns409() throws Exception {
                when(itemService.getAllItems(anyString()))
                        .thenThrow(new RuntimeException("failAll"));

                mvc.perform(get("/api/items/all")
                                .param("token", "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string("failAll"));
                }

                @Test @DisplayName("ADD REVIEW → RuntimeException → 409")
                void addReview_runtimeError_returns409() throws Exception {
                doThrow(new RuntimeException("badReview"))
                        .when(itemService).addReviewToItem(anyInt(), anyInt(),
                                                        anyString(), anyString());

                mvc.perform(post("/api/items/7/reviews")
                                .param("rating",     "5")
                                .param("reviewText","Good")
                                .param("token",      "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string("badReview"));
                }

                @Test @DisplayName("LIST REVIEWS → RuntimeException → 409")
                void listReviews_runtimeError_returns409() throws Exception {
                when(itemService.getItemReviews(anyInt(), anyString()))
                        .thenThrow(new RuntimeException("badList"));

                mvc.perform(get("/api/items/3/reviews")
                                .param("token", "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string("badList"));
                }

                @Test @DisplayName("GET RATING → RuntimeException → 409")
                void getRating_runtimeError_returns409() throws Exception {
                when(itemService.getItemAverageRating(anyInt(), anyString()))
                        .thenThrow(new RuntimeException("badRating"));

                mvc.perform(get("/api/items/8/rating")
                                .param("token", "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string("badRating"));
                }

                @Test @DisplayName("GET BY IDS → RuntimeException → 409")
                void getByIds_runtimeError_returns409() throws Exception {
                when(itemService.getItemsByIds(anyList(), anyString()))
                        .thenThrow(new RuntimeException("badByIds"));

                mvc.perform(post("/api/items/by-ids")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[1,2]")
                                .param("token", "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string("badByIds"));
                }

                @Test @DisplayName("GET BY CATEGORY → RuntimeException → 409")
                void getByCategory_runtimeError_returns409() throws Exception {
                when(itemService.getItemsByCategory(any(), anyString()))
                        .thenThrow(new RuntimeException("badCat"));

                mvc.perform(get("/api/items/category")
                                .param("category", "BOOKS")
                                .param("token",    "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string("badCat"));
                }
        }

        @Nested
        @DisplayName("11. ADDITIONAL EXCEPTION BRANCHES")
        class AdditionalBranches {

                // ─── 1. CREATE ITEM: OurRuntime → 409 + body ─────────────────
                @Test @DisplayName("CREATE → OurRuntime → 409 w/ message")
                void create_ourRuntimeError_returns409_body() throws Exception {
                when(itemService.createItem(
                        anyInt(), anyString(), anyString(),
                        eq(ItemCategory.ELECTRONICS), anyString()))
                        .thenThrow(new OurRuntime("conflictX"));

                mvc.perform(post("/api/items/create")
                                .param("shopId",      "1")
                                .param("name",        "X")
                                .param("description", "D")
                                .param("category",    "ELECTRONICS")
                                .param("token",       "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("conflictX")));
                }

                // ─── 2. GET ONE ITEM: plain RuntimeException → catch(RuntimeException) ───────────
                @Test @DisplayName("GET ONE → RuntimeException → 409 w/ message")
                void getOne_runtimeError_returns409_body() throws Exception {
                when(itemService.getItem(anyInt(), anyString()))
                        .thenThrow(new RuntimeException("rtErr"));

                mvc.perform(get("/api/items/5")
                                .param("token", "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string("rtErr"));
                }

                // ─── 3. GET ALL ITEMS: IllegalArgumentException → 400 + body ────────────────
                @Test @DisplayName("GET ALL → IllegalArgumentException → 400 w/ message")
                void getAll_badRequest_returns400_body() throws Exception {
                when(itemService.getAllItems(anyString()))
                        .thenThrow(new IllegalArgumentException("badAll"));

                mvc.perform(get("/api/items/all")
                                .param("token", "tok"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("badAll"));
                }

                // ─── 4. GET ALL ITEMS: OurRuntime → 409 + body ──────────────────────────────
                @Test @DisplayName("GET ALL → OurRuntime → 409 w/ message")
                void getAll_ourRuntimeError_returns409_body() throws Exception {
                when(itemService.getAllItems(anyString()))
                        .thenThrow(new OurRuntime("runtimeAll"));

                mvc.perform(get("/api/items/all")
                                .param("token", "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("runtimeAll")));
                }

                // ─── 5. ADD REVIEW: OurRuntime → 409 empty body ───────────────────────────
                @Test @DisplayName("ADD REVIEW → OurRuntime → 409 empty")
                void addReview_ourRuntimeError_returns409_emptyBody() throws Exception {
                doThrow(new OurRuntime("x"))
                        .when(itemService).addReviewToItem(anyInt(), anyInt(), anyString(), anyString());

                mvc.perform(post("/api/items/7/reviews")
                                .param("rating",     "5")
                                .param("reviewText","Good")
                                .param("token",      "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string(""));
                }

                // ─── 6. LIST REVIEWS: OurRuntime → 409 + body ─────────────────────────────
                @Test @DisplayName("LIST REVIEWS → OurRuntime → 409 w/ message")
                void listReviews_ourRuntimeError_returns409_body() throws Exception {
                when(itemService.getItemReviews(anyInt(), anyString()))
                        .thenThrow(new OurRuntime("badListRT"));

                mvc.perform(get("/api/items/3/reviews")
                                .param("token", "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("badListRT")));
                }

                // ─── 7. GET RATING: OurRuntime → 409 + body ───────────────────────────────
                @Test @DisplayName("GET RATING → OurRuntime → 409 w/ message")
                void getRating_ourRuntimeError_returns409_body() throws Exception {
                when(itemService.getItemAverageRating(anyInt(), anyString()))
                        .thenThrow(new OurRuntime("badRatingRT"));

                mvc.perform(get("/api/items/8/rating")
                                .param("token", "tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("badRatingRT")));
                }

                // ─── 8. GET BY IDS: OurRuntime → 409 + body ──────────────────────────────
                @Test @DisplayName("GET BY IDS → OurRuntime → 409 w/ message")
                void getByIds_ourRuntimeError_returns409_body() throws Exception {
                when(itemService.getItemsByIds(anyList(), anyString()))
                        .thenThrow(new OurRuntime("badByIdsRT"));

                mvc.perform(post("/api/items/by-ids")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[1,2]")
                                .param("token","tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("badByIdsRT")));
                }

                // ─── 9. GET BY CATEGORY: OurRuntime → 409 + body ──────────────────────────
                @Test @DisplayName("GET BY CATEGORY → OurRuntime → 409 w/ message")
                void getByCategory_ourRuntimeError_returns409_body() throws Exception {
                when(itemService.getItemsByCategory(any(), anyString()))
                        .thenThrow(new OurRuntime("badCatRT"));

                mvc.perform(get("/api/items/category")
                                .param("category","BOOKS")
                                .param("token","tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("badCatRT")));
                }

                // ─── 10. DELETE ITEM: OurRuntime → 409 empty body ─────────────────────────
                @Test @DisplayName("DELETE → OurRuntime → 409 empty")
                void delete_ourRuntime_returns409_emptyBody() throws Exception {
                doThrow(new OurRuntime("boom"))
                        .when(itemService).deleteItem(anyInt(), anyString());

                mvc.perform(delete("/api/items/1")
                                .param("token","tok"))
                .andExpect(status().isConflict())
                .andExpect(content().string(""));
                }
        }

}
