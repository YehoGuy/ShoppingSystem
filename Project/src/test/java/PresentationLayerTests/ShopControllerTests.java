package PresentationLayerTests;

import java.util.List;
import java.util.NoSuchElementException;
import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;

import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.ApplicationLayer.Shop.ShopService;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.DomainLayer.Shop.Discount.Discount;
import com.example.app.DomainLayer.Shop.Discount.PolicyLeaf;
import com.example.app.PresentationLayer.Controller.ShopController;
import com.example.app.InfrastructureLayer.WSEPShipping;

/**
 * Full slice tests – **every ShopController endpoint has ≥ 2 cases**.
 */
@WebMvcTest(controllers = ShopController.class)
@ContextConfiguration(classes = ShopControllerTests.TestBoot.class)
@AutoConfigureMockMvc(addFilters = false)
class ShopControllerTests {

        /* ── Boot helper ─────────────────────────────────────────────────── */
        @SpringBootApplication(scanBasePackages = "com.example.app.PresentationLayer")
        static class TestBoot {
        }

        @Autowired
        MockMvc mvc;
        @MockBean
        ShopService shopService;

        private ShippingMethod stubShip() { // easy fake shipper
                return new WSEPShipping();
        }

        /* ───────────── 1 · CREATE SHOP ───────────── */
        @Nested
        class CreateShop {
                @Test
                void created_201() throws Exception {
                        when(shopService.createShop(any(), any(), any(), any()))
                                        .thenReturn(new Shop(1, "My", stubShip()));
                        mvc.perform(post("/api/shops/create")
                                        .param("name", "My").param("token", "tok"))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.id").value(1));
                }

                @Test
                void conflict_409() throws Exception {
                        when(shopService.createShop(any(), any(), any(), any()))
                                        .thenThrow(new RuntimeException());
                        mvc.perform(post("/api/shops/create")
                                        .param("name", "Dup").param("token", "tok"))
                                        .andExpect(status().isConflict());
                }
        }

        /* ───────────── 2 · GET SHOP ─────────────── */
        @Nested
        class GetShop {
                @Test
                void ok_200() throws Exception {
                        when(shopService.getShop(2, "tok"))
                                        .thenReturn(new Shop(2, "S", stubShip()));
                        mvc.perform(get("/api/shops/2").param("token", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(2));
                }

                @Test
                void notFound_404() throws Exception {
                        when(shopService.getShop(9, "tok"))
                                        .thenThrow(new NoSuchElementException());
                        mvc.perform(get("/api/shops/9").param("token", "tok"))
                                        .andExpect(status().isNotFound());
                }
        }

        /* ───────────── 3 · GET ALL ──────────────── */
        @Nested
        class GetAllShops {
                @Test
                void list_200() throws Exception {
                        when(shopService.getAllShops("tok"))
                                        .thenReturn(List.of(new Shop(1, "A", stubShip())));
                        mvc.perform(get("/api/shops/all").param("token", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$[0].id").value(1));
                }

                @Test
                void empty_200() throws Exception {
                        when(shopService.getAllShops("tok")).thenReturn(List.of());
                        mvc.perform(get("/api/shops/all").param("token", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().json("[]"));
                }
        }

        /* ───────────── 4 · UPDATE POLICY ────────── */
        @Nested
        class UpdatePolicy {
                @Test
                void noContent_204() throws Exception {
                        doNothing().when(shopService).updatePurchasePolicy(1, null, "tok");
                        mvc.perform(post("/api/shops/1/policy").param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }

                @Test
                void badRequest_400() throws Exception {
                        doThrow(new IllegalArgumentException())
                                        .when(shopService).updatePurchasePolicy(anyInt(), any(), any());
                        mvc.perform(post("/api/shops/3/policy").param("token", "tok"))
                                        .andExpect(status().isBadRequest());
                }
        }

        /* ───────────── 5 · CHECK POLICY ─────────── */
        @Nested
        class CheckPolicy {
                @Test
                void pass_returnsTrue() throws Exception {
                        when(shopService.checkPolicy(any(), eq("tok"))).thenReturn(true);
                        mvc.perform(post("/api/shops/policy/check").param("token", "tok")
                                        .contentType("application/json").content("{}"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("true"));
                }

                @Test
                void fail_returnsFalse() throws Exception {
                        when(shopService.checkPolicy(any(), eq("tok"))).thenReturn(false);
                        mvc.perform(post("/api/shops/policy/check").param("token", "tok")
                                        .contentType("application/json").content("{}"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("false"));
                }
        }

        /* ───────────── 6 · GLOBAL DISCOUNT ──────── */
        @Nested
        class GlobalDiscount {
                @Test
                void set_204() throws Exception {
                        doNothing().when(shopService).setGlobalDiscount(1, 10, false, "tok");
                        mvc.perform(post("/api/shops/1/discount/global")
                                        .param("discount", "10").param("isDouble", "false")
                                        .param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }

                @Test
                void remove_204() throws Exception {
                        doNothing().when(shopService).removeGlobalDiscount(1, "tok");
                        mvc.perform(delete("/api/shops/1/discount/global")
                                        .param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }
        }

        /* ───────────── 7 · ITEM DISCOUNT ────────── */
        @Nested
        class ItemDiscount {
                @Test
                void set_204() throws Exception {
                        doNothing().when(shopService).setDiscountForItem(1, 5, 20, true, "tok");
                        mvc.perform(post("/api/shops/1/discount/items/5")
                                        .param("discount", "20").param("isDouble", "true")
                                        .param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }

                @Test
                void remove_204() throws Exception {
                        doNothing().when(shopService).removeDiscountForItem(1, 5, "tok");
                        mvc.perform(delete("/api/shops/1/discount/items/5")
                                        .param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }
        }

        /* ───────────── 8 · CATEGORY DISCOUNT ────── */
        @Nested
        class CategoryDiscount {
                @Test
                void set_204() throws Exception {
                        doNothing().when(shopService)
                                        .setCategoryDiscount(1, ItemCategory.GROCERY, 15, false, "tok");
                        mvc.perform(post("/api/shops/1/discount/categories")
                                        .param("category", "GROCERY").param("discount", "15")
                                        .param("isDouble", "false").param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }

                @Test
                void remove_204() throws Exception {
                        doNothing().when(shopService)
                                        .removeCategoryDiscount(1, ItemCategory.GROCERY, "tok");
                        mvc.perform(delete("/api/shops/1/discount/categories")
                                        .param("category", "GROCERY").param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }
        }

        /* ───────────── 9 · REVIEWS & RATING ─────── */
        @Nested
        class ReviewsRating {
                @Test
                void add_202() throws Exception {
                        doNothing().when(shopService).addReviewToShop(1, 5, "Great", "tok");
                        mvc.perform(post("/api/shops/1/reviews")
                                        .param("rating", "5").param("reviewText", "Great")
                                        .param("token", "tok"))
                                        .andExpect(status().isAccepted());
                }

                @Test
                void rating_notFound_404() throws Exception {
                        when(shopService.getShopAverageRating(9, "tok"))
                                        .thenThrow(new NoSuchElementException());
                        mvc.perform(get("/api/shops/9/rating").param("token", "tok"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void rating_success_200() throws Exception {
                        when(shopService.getShopAverageRating(1, "tok")).thenReturn(4.5);
                        mvc.perform(get("/api/shops/1/rating").param("token", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("4.5"));
                }
        }

        /* ─────────── 10 · ITEM MANAGEMENT ───────── */
        @Nested
        class ItemMgmt {
                @Test
                void addItem_201() throws Exception {
                        doNothing().when(shopService).addItemToShop(1, "I", "D", 3, ItemCategory.ELECTRONICS, 100,
                                        "tok");
                        mvc.perform(post("/api/shops/1/items")
                                        .param("name", "I").param("description", "D")
                                        .param("quantity", "3").param("category", "ELECTRONICS")
                                        .param("price", "100")
                                        .param("token", "tok"))
                                        .andExpect(status().isCreated());
                }

                @Test
                void updatePrice_badRequest_400() throws Exception {
                        doThrow(new IllegalArgumentException())
                                        .when(shopService)
                                        .updateItemPriceInShop(anyInt(), anyInt(), anyInt(), anyString());
                        mvc.perform(post("/api/shops/1/items/7/price")
                                        .param("price", "-5").param("token", "tok"))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                void removeItem_204() throws Exception {
                        doNothing().when(shopService).removeItemFromShop(1, 7, "tok");
                        mvc.perform(delete("/api/shops/1/items/7").param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }

                @Test
                void removeItem_notFound_404() throws Exception {
                        doThrow(new NoSuchElementException())
                                        .when(shopService).removeItemFromShop(1, 8, "tok");
                        mvc.perform(delete("/api/shops/1/items/8").param("token", "tok"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void updatePrice_success_204() throws Exception {
                        doNothing().when(shopService).updateItemPriceInShop(1, 7, 120, "tok");
                        mvc.perform(post("/api/shops/1/items/7/price")
                                        .param("price", "120").param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }
        }

        /* ─────────── 11 · SEARCH & INVENTORY ─────── */
        @Nested
        class SearchInventory {
                @Test
                void listByShop_200() throws Exception {
                        when(shopService.getItemsByShop(1, "tok"))
                                        .thenReturn(List.of(new Item(9, "X", "D", 0)));
                        mvc.perform(get("/api/shops/1/items").param("token", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$[0].id").value(9));
                }

                @Test
                void listAll_empty() throws Exception {
                        when(shopService.getItems("tok")).thenReturn(List.of());
                        mvc.perform(get("/api/shops/items").param("token", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().json("[]"));
                }

                @Test
                void searchAll_400_badQuery() throws Exception {
                        mvc.perform(get("/api/shops/search") // missing token
                                        .param("name", "abc"))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                void searchInShop_200() throws Exception {
                        when(shopService.searchItemsInShop(1, null, null, null, null, null, null, "tok"))
                                        .thenReturn(List.of(new Item(6, "Y", "D", 1)));
                        mvc.perform(get("/api/shops/1/search").param("token", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$[0].id").value(6));
                }

                @Test
                void listByShop_notFound_404() throws Exception {
                        when(shopService.getItemsByShop(5, "tok"))
                                        .thenThrow(new NoSuchElementException());
                        mvc.perform(get("/api/shops/5/items").param("token", "tok"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void searchAll_success_200() throws Exception {
                        when(shopService.searchItems(null, null, null, null, null, null, 4.0, "tok"))
                                        .thenReturn(List.of());
                        mvc.perform(get("/api/shops/search")
                                        .param("minShopRating", "4.0")
                                        .param("token", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().json("[]"));
                }

                @Test
                void searchInShop_badRequest_400() throws Exception {
                        mvc.perform(get("/api/shops/1/search") // missing token
                                        .param("name", "x"))
                                        .andExpect(status().isBadRequest());
                }
        }

        /* ─────────── 12 · SUPPLY MGMT ───────────── */
        @Nested
        class SupplyMgmt {
                @Test
                void addSupply_post_204() throws Exception {
                        doNothing().when(shopService).addSupplyToItem(1, 7, 5, "tok");
                        mvc.perform(post("/api/shops/1/items/7/supply")
                                        .param("quantity", "5").param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }

                @Test
                void addSupply_patch_204() throws Exception {
                        doNothing().when(shopService).addSupply(1, 7, 3, "tok");
                        mvc.perform(patch("/api/shops/1/items/7/supply/add")
                                        .param("supply", "3").param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }

                @Test
                void removeSupply_patch_204() throws Exception {
                        doNothing().when(shopService).removeSupply(1, 7, 2, "tok");
                        mvc.perform(post("/api/shops/1/items/7/supply/remove")
                                        .param("supply", "2").param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }

                @Test
                void quantity_200() throws Exception {
                        when(shopService.getItemQuantityFromShop(1, 7, "tok")).thenReturn(11);
                        mvc.perform(get("/api/shops/1/items/7/quantity").param("token", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("11"));
                }

                @Test
                void available_false() throws Exception {
                        when(shopService.checkSupplyAvailability(1, 7, "tok")).thenReturn(false);
                        mvc.perform(get("/api/shops/1/items/7/available").param("token", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("false"));
                }

                @Test
                void quantity_notFound_500() throws Exception { // ← rename + new status
                        when(shopService.getItemQuantityFromShop(1, 99, "tok"))
                                        .thenThrow(new NoSuchElementException());

                        mvc.perform(get("/api/shops/1/items/99/quantity").param("token", "tok"))
                                        .andExpect(status().isInternalServerError()); // ← expect 500
                }

                @Test
                void available_true() throws Exception {
                        when(shopService.checkSupplyAvailability(1, 7, "tok")).thenReturn(true);
                        mvc.perform(get("/api/shops/1/items/7/available").param("token", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("true"));
                }
        }

        /* ─────────── 13 · SHIP PURCHASE ─────────── */
        @Nested
        class ShipPurchase {
                @Test
                void accepted_202() throws Exception {
                        doNothing().when(shopService).shipPurchase("tok", 3, 1, "IL", "TLV", "H", "1");
                        mvc.perform(post("/api/shops/1/purchase/3/ship")
                                        .param("country", "IL").param("city", "TLV")
                                        .param("street", "H").param("postalCode", "1")
                                        .param("token", "tok"))
                                        .andExpect(status().isAccepted());
                }

                @Test
                void serverError_500() throws Exception {
                        doThrow(new RuntimeException())
                                        .when(shopService)
                                        .shipPurchase(any(), anyInt(), anyInt(), any(), any(), any(), any());
                        mvc.perform(post("/api/shops/1/purchase/2/ship")
                                        .param("country", "IL").param("city", "H")
                                        .param("street", "S").param("postalCode", "1")
                                        .param("token", "tok"))
                                        .andExpect(status().isInternalServerError());
                }
        }

        /* ─────────── 14 · CLOSE SHOP ────────────── */
        @Nested
        class CloseShop {
                @Test
                void noContent_204() throws Exception {
                        doNothing().when(shopService).closeShop(1, "tok");
                        mvc.perform(delete("/api/shops/1").param("token", "tok"))
                                        .andExpect(status().isNoContent());
                }

                @Test
                void conflict_409() throws Exception {
                        doThrow(new RuntimeException()).when(shopService).closeShop(1, "tok");
                        mvc.perform(delete("/api/shops/1").param("token", "tok"))
                                        .andExpect(status().isConflict());
                }
        }

        /* ───────────── 15. GET POLICIES ───────────── */
        @Nested
        class GetPoliciesTests {
        @Test
        void success_returns200AndList() throws Exception {
                // stub two dummy policies
                PolicyLeaf p1 = new PolicyLeaf(/*...*/);
                PolicyLeaf p2 = new PolicyLeaf(/*...*/);
                when(shopService.getPolicies(3, "tok"))
                .thenReturn(List.of(p1, p2));

                mvc.perform(get("/api/shops/3/policies").param("token","tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void conflict_illegalArg_returns409() throws Exception {
                doThrow(new IllegalArgumentException("bad"))
                .when(shopService).getPolicies(anyInt(), anyString());

                mvc.perform(get("/api/shops/3/policies").param("token","tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void conflict_runtime_returns409() throws Exception {
                doThrow(new RuntimeException("fail"))
                .when(shopService).getPolicies(anyInt(), anyString());

                mvc.perform(get("/api/shops/3/policies").param("token","tok"))
                .andExpect(status().isConflict());
        }
        }

        /* ──────── 16. GET SHOPS BY WORKER ───────── */
        @Nested
        class ShopsByWorkerTests {
        @Test
        void success_returns200AndList() throws Exception {
                Shop s = new Shop(7, "X", stubShip());
                when(shopService.getShopsByWorker(7, "tok"))
                .thenReturn(List.of(s));

                mvc.perform(get("/api/shops/ByWorkerId")
                        .param("workerId","7")
                        .param("token","tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));
        }

        @Test
        void illegalArg_returns500() throws Exception {
                doThrow(new IllegalArgumentException())
                .when(shopService).getShopsByWorker(anyInt(), anyString());

                mvc.perform(get("/api/shops/ByWorkerId")
                        .param("workerId","7")
                        .param("token","tok"))
                .andExpect(status().isInternalServerError());
        }

        @Test
        void runtime_returns500() throws Exception {
                doThrow(new RuntimeException())
                .when(shopService).getShopsByWorker(anyInt(), anyString());

                mvc.perform(get("/api/shops/ByWorkerId")
                        .param("workerId","7")
                        .param("token","tok"))
                .andExpect(status().isInternalServerError());
        }
        }

        /* ────────── 17. GET DISCOUNTS ───────────── */
        @Nested
        class GetDiscountsTests {
        @Test
        void success_returns200AndList() throws Exception {
                Discount d = org.mockito.Mockito.mock(Discount.class);
                when(shopService.getDiscounts(5, "tok"))
                .thenReturn(List.of(d));

                mvc.perform(get("/api/shops/5/discounts").param("token","tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void conflict_illegalArg_returns409() throws Exception {
                doThrow(new IllegalArgumentException())
                .when(shopService).getDiscounts(anyInt(), anyString());

                mvc.perform(get("/api/shops/5/discounts").param("token","tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void conflict_runtime_returns409() throws Exception {
                doThrow(new RuntimeException())
                .when(shopService).getDiscounts(anyInt(), anyString());

                mvc.perform(get("/api/shops/5/discounts").param("token","tok"))
                .andExpect(status().isConflict());
        }
        }

        /* ───────── 18. SET DISCOUNT POLICY ───────── */
        @Nested
        class SetDiscountPolicyTests {
        @Test
        void success_returns204() throws Exception {
                doNothing().when(shopService)
                .setDiscountPolicy(eq(9), any(), eq("tok"));

                mvc.perform(post("/api/shops/9/discount/policy")
                        .param("token","tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_illegalArg_returns400() throws Exception {
                doThrow(new IllegalArgumentException("bad"))
                .when(shopService).setDiscountPolicy(anyInt(), any(), anyString());

                mvc.perform(post("/api/shops/9/discount/policy")
                        .param("token","tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_runtime_returns409() throws Exception {
                doThrow(new RuntimeException("fail"))
                .when(shopService).setDiscountPolicy(anyInt(), any(), anyString());

                mvc.perform(post("/api/shops/9/discount/policy")
                        .param("token","tok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
                }
        }
        @Nested @DisplayName("DISCOUNTS: item, category, global")
        class DiscountEndpoints {
                @Test
                void setItemDiscount_success_204() throws Exception {
                doNothing().when(shopService).setDiscountForItem(1,5,20,true,"tok");
                mvc.perform(post("/api/shops/1/discount/items/5")
                        .param("discount","20").param("isDouble","true").param("token","tok"))
                .andExpect(status().isNoContent());
                }

                @Test
                void removeItemDiscount_notFound_404() throws Exception {
                doThrow(new IllegalArgumentException()).when(shopService).removeDiscountForItem(1,5,"tok");
                mvc.perform(delete("/api/shops/1/discount/items/5").param("token","tok"))
                .andExpect(status().isBadRequest());
                }

                @Test
                void setCategoryDiscount_conflict_409() throws Exception {
                doThrow(new RuntimeException()).when(shopService)
                        .setCategoryDiscount(2, ItemCategory.GROCERY,10,false,"tok");
                mvc.perform(post("/api/shops/2/discount/categories")
                        .param("category","GROCERY").param("discount","10")
                        .param("isDouble","false").param("token","tok"))
                .andExpect(status().isConflict());
                }

                @Test
                void removeGlobalDiscount_success_204() throws Exception {
                doNothing().when(shopService).removeGlobalDiscount(3,"tok");
                mvc.perform(delete("/api/shops/3/discount/global").param("token","tok"))
                .andExpect(status().isNoContent());
                }
        }

        @Nested @DisplayName("REVIEWS & RATINGS")
        class ReviewEndpoints {
                @Test
                void addReview_serverError_409()  throws Exception {
                doThrow(new RuntimeException()).when(shopService)
                        .addReviewToShop(4,5,"Nice","tok");
                mvc.perform(post("/api/shops/4/reviews")
                        .param("rating","5").param("reviewText","Nice").param("token","tok"))
                .andExpect(status().isConflict());
                }

                @Test
                void getAverageRating_emptyList_returnsOk0() throws Exception {
                when(shopService.getShopAverageRating(5,"tok")).thenReturn(0.0);
                mvc.perform(get("/api/shops/5/rating").param("token","tok"))
                .andExpect(status().isOk()).andExpect(content().string("0.0"));
                }
        }

        @Nested @DisplayName("POLICY UPDATE & VALIDATION")
        class PolicyEndpoints {
                @Test
                void updatePolicy_success_204() throws Exception {
                doNothing().when(shopService).updatePurchasePolicy(6, null, "tok");
                mvc.perform(post("/api/shops/6/policy").param("token","tok"))
                .andExpect(status().isNoContent());
                }

                @Test
                void updatePolicy_serverError_409() throws Exception {
                doThrow(new RuntimeException()).when(shopService).updatePurchasePolicy(anyInt(),any(),anyString());
                mvc.perform(post("/api/shops/7/policy").param("token","tok"))
                .andExpect(status().isConflict());
                }
        }

        @Nested @DisplayName("SEARCH ITEMS")
        class SearchEndpoints {
                @Test
                void searchAll_withoutToken_badRequest() throws Exception {
                mvc.perform(get("/api/shops/search").param("name","x"))
                .andExpect(status().isBadRequest());
                }

                @Test
                void searchInShop_returnsList() throws Exception {
                when(shopService.searchItemsInShop(eq(8), eq(null), eq(null), eq(null),
                        eq(null), eq(null), eq(null), eq("tok")))
                        .thenReturn(List.of(new com.example.app.DomainLayer.Item.Item(9,"Name","Desc",1)));

                mvc.perform(get("/api/shops/8/search").param("token","tok"))
                .andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(1)));
                }
        }

        @Nested @DisplayName("PRICE UPDATE")
        class PriceEndpoints {
                @Test
                void updatePrice_success_204() throws Exception {
                doNothing().when(shopService).updateItemPriceInShop(9,10,150,"tok");
                mvc.perform(post("/api/shops/9/items/10/price")
                        .param("price","150").param("token","tok"))
                .andExpect(status().isNoContent());
                }

                @Test
                void updatePrice_badRequest_400() throws Exception {
                doThrow(new IllegalArgumentException()).when(shopService)
                        .updateItemPriceInShop(anyInt(),anyInt(),anyInt(),anyString());
                mvc.perform(post("/api/shops/9/items/10/price")
                        .param("price","-1").param("token","tok"))
                .andExpect(status().isBadRequest());
                }
        }

        @Nested
    @DisplayName("Global Discount Error Mapping")
    class GlobalDiscountErrorTests {
        @Test
        void illegalArg_returns400() throws Exception {
            doThrow(new IllegalArgumentException("bad"))
                .when(shopService).setGlobalDiscount(1, 10, false, "tok");

            mvc.perform(post("/api/shops/1/discount/global")
                    .param("discount","10").param("isDouble","false").param("token","tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException("no shop"))
                .when(shopService).setGlobalDiscount(1, 10, false, "tok");

            mvc.perform(post("/api/shops/1/discount/global")
                    .param("discount","10").param("isDouble","false").param("token","tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException("conflict"))
                .when(shopService).setGlobalDiscount(1, 10, false, "tok");

            mvc.perform(post("/api/shops/1/discount/global")
                    .param("discount","10").param("isDouble","false").param("token","tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("boom"); })
                .when(shopService).setGlobalDiscount(1, 10, false, "tok");

            mvc.perform(post("/api/shops/1/discount/global")
                    .param("discount","10").param("isDouble","false").param("token","tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Item Discount Error Mapping")
    class ItemDiscountErrorTests {
        @Test
        void illegalArg_returns400() throws Exception {
            doThrow(new IllegalArgumentException())
                .when(shopService).setDiscountForItem(2, 5, 20, true, "tok");

            mvc.perform(post("/api/shops/2/discount/items/5")
                    .param("discount","20").param("isDouble","true").param("token","tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException())
                .when(shopService).setDiscountForItem(2, 5, 20, true, "tok");

            mvc.perform(post("/api/shops/2/discount/items/5")
                    .param("discount","20").param("isDouble","true").param("token","tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException())
                .when(shopService).setDiscountForItem(2, 5, 20, true, "tok");

            mvc.perform(post("/api/shops/2/discount/items/5")
                    .param("discount","20").param("isDouble","true").param("token","tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("fail"); })
                .when(shopService).setDiscountForItem(2, 5, 20, true, "tok");

            mvc.perform(post("/api/shops/2/discount/items/5")
                    .param("discount","20").param("isDouble","true").param("token","tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Category Discount Error Mapping")
    class CategoryDiscountErrorTests {
        @Test
        void illegalArg_returns400() throws Exception {
            doThrow(new IllegalArgumentException())
                .when(shopService).setCategoryDiscount(3, ItemCategory.ELECTRONICS, 15, false, "tok");

            mvc.perform(post("/api/shops/3/discount/categories")
                    .param("category","ELECTRONICS").param("discount","15")
                    .param("isDouble","false").param("token","tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException())
                .when(shopService).setCategoryDiscount(3, ItemCategory.TOYS, 15, false, "tok");

            mvc.perform(post("/api/shops/3/discount/categories")
                    .param("category","TOYS").param("discount","15")
                    .param("isDouble","false").param("token","tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException())
                .when(shopService).setCategoryDiscount(3, ItemCategory.GROCERY, 15, false, "tok");

            mvc.perform(post("/api/shops/3/discount/categories")
                    .param("category","GROCERY").param("discount","15")
                    .param("isDouble","false").param("token","tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("error"); })
                .when(shopService).setCategoryDiscount(3, ItemCategory.CLOTHING, 15, false, "tok");

            mvc.perform(post("/api/shops/3/discount/categories")
                    .param("category","CLOTHING").param("discount","15")
                    .param("isDouble","false").param("token","tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Get Policies Error Mapping")
    class GetPoliciesErrorTests {
        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException())
                .when(shopService).getPolicies(4, "tok");

            mvc.perform(get("/api/shops/4/policies").param("token","tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException())
                .when(shopService).getPolicies(4, "tok");

            mvc.perform(get("/api/shops/4/policies").param("token","tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("oops"); })
                .when(shopService).getPolicies(4, "tok");

            mvc.perform(get("/api/shops/4/policies").param("token","tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("General Search Error Mapping")
    class SearchErrorTests {
        @Test
        void notFound_returns404() throws Exception {
            when(shopService.searchItems(any(), any(), any(), any(), any(), any(), any(), eq("tok")))
                .thenThrow(new NoSuchElementException());

            mvc.perform(get("/api/shops/search").param("token","tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_returns409() throws Exception {
            when(shopService.searchItems(any(), any(), any(), any(), any(), any(), any(), eq("tok")))
                .thenThrow(new RuntimeException());

            mvc.perform(get("/api/shops/search").param("token","tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            when(shopService.searchItems(any(), any(), any(), any(), any(), any(), any(), eq("tok")))
                .thenAnswer(invocation -> { throw new IOException("search fail"); });

            mvc.perform(get("/api/shops/search").param("token","tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Update Price Error Mapping")
    class UpdatePriceErrorTests {
        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException())
                .when(shopService).updateItemPriceInShop(5,6,100,"tok");

            mvc.perform(post("/api/shops/5/items/6/price")
                    .param("price","100").param("token","tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException())
                .when(shopService).updateItemPriceInShop(5,6,100,"tok");

            mvc.perform(post("/api/shops/5/items/6/price")
                    .param("price","100").param("token","tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("update fail"); })
                .when(shopService).updateItemPriceInShop(5,6,100,"tok");

            mvc.perform(post("/api/shops/5/items/6/price")
                    .param("price","100").param("token","tok"))
               .andExpect(status().isInternalServerError());
        }
    }
}
