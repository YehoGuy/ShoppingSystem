package PresentationLayerTests;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.ApplicationLayer.Shop.ShopService;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.PresentationLayer.Controller.ShopController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive slice tests for ShopController.
 */
@WebMvcTest(controllers = ShopController.class)
@ContextConfiguration(classes = ShopControllerTests.TestBootApp.class)
@AutoConfigureMockMvc(addFilters = false)
class ShopControllerTests {

    @SpringBootApplication(scanBasePackages = "com.example.app.PresentationLayer")
    static class TestBootApp {
    }

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ShopService shopService;

    private ShippingMethod stubShippingMethod() {
        return new ShippingMethod() {
            @Override
            public void processShipment(int purchaseId, String country, String city, String street, String postalCode) {
            }

            @Override
            public String getDetails() {
                return "STANDARD";
            }
        };
    }

    @Nested
    @DisplayName("1. CREATE SHOP")
    class CreateShop {
        @Test
        void success_returnsCreated() throws Exception {
            ShippingMethod sm = stubShippingMethod();
            Shop dummy = new Shop(1, "MyShop", sm);
            when(shopService.createShop(eq("MyShop"), isNull(), isNull(), eq("tok")))
                    .thenReturn(dummy);

            mvc.perform(post("/api/shops/create")
                    .param("token", "tok")
                    .param("name", "MyShop"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    @DisplayName("2. GET SHOP")
    class GetShop {
        @Test
        void found_returnsOk() throws Exception {
            ShippingMethod sm = stubShippingMethod();
            Shop s = new Shop(2, "Shop2", sm);
            when(shopService.getShop(2, "tok")).thenReturn(s);

            mvc.perform(get("/api/shops/2").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2));
        }
    }

    @Test
    @DisplayName("3. GET ALL SHOPS")
    void getAll_returnsList() throws Exception {
        ShippingMethod sm = stubShippingMethod();
        Shop s1 = new Shop(1, "A", sm);
        Shop s2 = new Shop(2, "B", sm);
        when(shopService.getAllShops("tok")).thenReturn(List.of(s1, s2));

        mvc.perform(get("/api/shops/all").param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Nested
    @DisplayName("4. UPDATE POLICY")
    class UpdatePolicy {
        @Test
        void success_returnsNoContent() throws Exception {
            doNothing().when(shopService).updatePurchasePolicy(eq(1), isNull(), eq("tok"));
            mvc.perform(post("/api/shops/1/policy").param("token", "tok"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("5. GLOBAL DISCOUNT")
    class GlobalDiscount {
        @Test
        void set_returnsNoContent() throws Exception {
            doNothing().when(shopService).setGlobalDiscount(1, 10, false, "tok");
            mvc.perform(post("/api/shops/1/discount/global")
                    .param("discount", "10")
                    .param("isDouble", "false")
                    .param("token", "tok"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void remove_returnsNoContent() throws Exception {
            doNothing().when(shopService).removeGlobalDiscount(1, "tok");
            mvc.perform(delete("/api/shops/1/discount/global").param("token", "tok"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("6. ITEM DISCOUNT")
    class ItemDiscount {
        @Test
        void set_returnsNoContent() throws Exception {
            doNothing().when(shopService).setDiscountForItem(1, 5, 20, true, "tok");
            mvc.perform(post("/api/shops/1/discount/items/5")
                    .param("discount", "20")
                    .param("isDouble", "true")
                    .param("token", "tok"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void remove_returnsNoContent() throws Exception {
            doNothing().when(shopService).removeDiscountForItem(1, 5, "tok");
            mvc.perform(delete("/api/shops/1/discount/items/5").param("token", "tok"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("7. CATEGORY DISCOUNT")
    class CategoryDiscount {
        @Test
        void set_returnsNoContent() throws Exception {
            doNothing().when(shopService).setCategoryDiscount(eq(1), eq(ItemCategory.BOOKS), eq(15), eq(false),
                    eq("tok"));
            mvc.perform(post("/api/shops/1/discount/categories")
                    .param("category", "BOOKS")
                    .param("discount", "15")
                    .param("isDouble", "false")
                    .param("token", "tok"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void remove_returnsNoContent() throws Exception {
            doNothing().when(shopService).removeCategoryDiscount(1, ItemCategory.BOOKS, "tok");
            mvc.perform(delete("/api/shops/1/discount/categories")
                    .param("category", "BOOKS")
                    .param("token", "tok"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("8. REVIEWS & RATING")
    class Reviews {
        @Test
        void addReview_returnsAccepted() throws Exception {
            doNothing().when(shopService).addReviewToShop(1, 5, "Great!", "tok");
            mvc.perform(post("/api/shops/1/reviews")
                    .param("rating", "5")
                    .param("reviewText", "Great!")
                    .param("token", "tok"))
                    .andExpect(status().isAccepted());
        }

        @Test
        void getRating_returnsOk() throws Exception {
            when(shopService.getShopAverageRating(1, "tok")).thenReturn(4.2);
            mvc.perform(get("/api/shops/1/rating").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("4.2"));
        }
    }

    @Nested
    @DisplayName("9. ITEM MANAGEMENT")
    class ItemManagement {
        @Test
        void addItem_returnsCreated() throws Exception {
            doNothing().when(shopService).addItemToShop(1, "Item", "Desc", 3, 100, "tok");
            mvc.perform(post("/api/shops/1/items")
                    .param("name", "Item")
                    .param("description", "Desc")
                    .param("quantity", "3")
                    .param("price", "100")
                    .param("token", "tok"))
                    .andExpect(status().isCreated());
        }

        @Test
        void updatePrice_returnsNoContent() throws Exception {
            doNothing().when(shopService).updateItemPriceInShop(1, 7, 50, "tok");
            mvc.perform(patch("/api/shops/1/items/7/price")
                    .param("price", "50")
                    .param("token", "tok"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void removeItem_returnsNoContent() throws Exception {
            doNothing().when(shopService).removeItemFromShop(1, 7, "tok");
            mvc.perform(delete("/api/shops/1/items/7").param("token", "tok"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("10. SEARCH & INVENTORY")
    class SearchInventory {
        @Test
        void listItemsByShop_returnsList() throws Exception {
            Item it = new Item(9, "X", "D", 0);
            when(shopService.getItemsByShop(1, "tok")).thenReturn(List.of(it));
            mvc.perform(get("/api/shops/1/items").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(9));
        }

        @Test
        void listAllItems_returnsList() throws Exception {
            Item it = new Item(8, "Y", "D2", 1);
            when(shopService.getItems("tok")).thenReturn(List.of(it));
            mvc.perform(get("/api/shops/items").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(8));
        }

        @Test
        void searchItems_returnsList() throws Exception {
            when(shopService.searchItems(null, null, null, null, null, null, null, "tok"))
                    .thenReturn(List.of(new Item(5, "Z", "D3", 3)));
            mvc.perform(get("/api/shops/search").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(5));
        }

        @Test
        void searchInShop_returnsList() throws Exception {
            when(shopService.searchItemsInShop(1, null, null, null, null, null, null, "tok"))
                    .thenReturn(List.of(new Item(6, "W", "D4", 4)));
            mvc.perform(get("/api/shops/1/search").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(6));
        }
    }

    @Test
    @DisplayName("11. CLOSE SHOP")
    void close_returnsNoContent() throws Exception {
        doNothing().when(shopService).closeShop(1, "tok");
        mvc.perform(delete("/api/shops/1").param("token", "tok"))
                .andExpect(status().isNoContent());
    }
}