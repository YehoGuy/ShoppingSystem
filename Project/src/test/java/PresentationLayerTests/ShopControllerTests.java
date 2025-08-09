package PresentationLayerTests;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.hasSize;
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

import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.ApplicationLayer.Shop.ShopService;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Discount.Discount;
import com.example.app.DomainLayer.Shop.Discount.PolicyLeaf;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.InfrastructureLayer.WSEPShipping;
import com.example.app.PresentationLayer.Controller.ShopController;

import jakarta.validation.ConstraintViolationException;

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

    @Nested
    @DisplayName("Remove Category Discount Error Mapping")
    class RemoveCategoryDiscountErrorTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(shopService).removeCategoryDiscount(1, ItemCategory.ELECTRONICS, "tok");
            
            mvc.perform(delete("/api/shops/1/discount/categories")
                    .param("category", "ELECTRONICS")
                    .param("token", "tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void illegalArg_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid category"))
                .when(shopService).removeCategoryDiscount(1, ItemCategory.GROCERY, "tok");

            mvc.perform(delete("/api/shops/1/discount/categories")
                    .param("category", "GROCERY")
                    .param("token", "tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void constraintViolation_returns400() throws Exception {
            doThrow(new ConstraintViolationException("Validation failed", null))
                .when(shopService).removeCategoryDiscount(2, ItemCategory.TOYS, "tok");

            mvc.perform(delete("/api/shops/2/discount/categories")
                    .param("category", "TOYS")
                    .param("token", "tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException("Shop not found"))
                .when(shopService).removeCategoryDiscount(999, ItemCategory.CLOTHING, "tok");

            mvc.perform(delete("/api/shops/999/discount/categories")
                    .param("category", "CLOTHING")
                    .param("token", "tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void runtimeException_returns409() throws Exception {
            doThrow(new RuntimeException("Discount removal conflict"))
                .when(shopService).removeCategoryDiscount(3, ItemCategory.SPORTS, "tok");

            mvc.perform(delete("/api/shops/3/discount/categories")
                    .param("category", "SPORTS")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("Database error"); })
                .when(shopService).removeCategoryDiscount(4, ItemCategory.BOOKS, "tok");

            mvc.perform(delete("/api/shops/4/discount/categories")
                    .param("category", "BOOKS")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Remove Global Discount Error Mapping")
    class RemoveGlobalDiscountErrorTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(shopService).removeGlobalDiscount(1, "tok");
            
            mvc.perform(delete("/api/shops/1/discount/global")
                    .param("token", "tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void illegalArg_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid shop"))
                .when(shopService).removeGlobalDiscount(1, "tok");

            mvc.perform(delete("/api/shops/1/discount/global")
                    .param("token", "tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void constraintViolation_returns400() throws Exception {
            doThrow(new ConstraintViolationException("Validation error", null))
                .when(shopService).removeGlobalDiscount(2, "tok");

            mvc.perform(delete("/api/shops/2/discount/global")
                    .param("token", "tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException("Shop not found"))
                .when(shopService).removeGlobalDiscount(999, "tok");

            mvc.perform(delete("/api/shops/999/discount/global")
                    .param("token", "tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void runtimeException_returns409() throws Exception {
            doThrow(new RuntimeException("Discount conflict"))
                .when(shopService).removeGlobalDiscount(3, "tok");

            mvc.perform(delete("/api/shops/3/discount/global")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("System error"); })
                .when(shopService).removeGlobalDiscount(4, "tok");

            mvc.perform(delete("/api/shops/4/discount/global")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Get Open Shops By Worker Tests")
    class GetOpenShopsByWorkerTests {
        @Test
        void success_returnsShopList() throws Exception {
            Shop shop1 = new Shop(1, "Open Shop 1", stubShip());
            Shop shop2 = new Shop(2, "Open Shop 2", stubShip());
            when(shopService.getOpenShopsByWorker(1, "tok"))
                .thenReturn(List.of(shop1, shop2));
            when(shopService.getItems("tok"))
                .thenReturn(List.of(new Item(10, "Item1", "Desc1", 5)));

            mvc.perform(get("/api/shops/ByWorkerId-open")
                    .param("workerId", "1")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[0].id").value(1))
               .andExpect(jsonPath("$[1].id").value(2));
        }

        @Test
        void emptyList_returnsEmptyArray() throws Exception {
            when(shopService.getOpenShopsByWorker(2, "tok"))
                .thenReturn(List.of());
            when(shopService.getItems("tok"))
                .thenReturn(List.of());

            mvc.perform(get("/api/shops/ByWorkerId-open")
                    .param("workerId", "2")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }

        @Test
        void exception_returns500() throws Exception {
            when(shopService.getOpenShopsByWorker(3, "tok"))
                .thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/shops/ByWorkerId-open")
                    .param("workerId", "3")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Get Closed Shops By Worker Tests")
    class GetClosedShopsByWorkerTests {
        @Test
        void success_returnsShopList() throws Exception {
            Shop shop1 = new Shop(5, "Closed Shop 1", stubShip());
            Shop shop2 = new Shop(6, "Closed Shop 2", stubShip());
            when(shopService.getClosedShopsByWorker(1, "tok"))
                .thenReturn(List.of(shop1, shop2));
            when(shopService.getItems("tok"))
                .thenReturn(List.of(new Item(20, "Item2", "Desc2", 3)));

            mvc.perform(get("/api/shops/ByWorkerId-closed")
                    .param("workerId", "1")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[0].id").value(5))
               .andExpect(jsonPath("$[1].id").value(6));
        }

        @Test
        void emptyList_returnsEmptyArray() throws Exception {
            when(shopService.getClosedShopsByWorker(2, "tok"))
                .thenReturn(List.of());
            when(shopService.getItems("tok"))
                .thenReturn(List.of());

            mvc.perform(get("/api/shops/ByWorkerId-closed")
                    .param("workerId", "2")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }

        @Test
        void exception_returns500() throws Exception {
            when(shopService.getClosedShopsByWorker(3, "tok"))
                .thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/shops/ByWorkerId-closed")
                    .param("workerId", "3")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Get All Open Shops Tests")
    class GetAllOpenShopsTests {
        @Test
        void success_returnsShopList() throws Exception {
            Shop shop1 = new Shop(10, "Global Open 1", stubShip());
            Shop shop2 = new Shop(11, "Global Open 2", stubShip());
            when(shopService.getAllOpenShops("tok"))
                .thenReturn(List.of(shop1, shop2));
            when(shopService.getItems("tok"))
                .thenReturn(List.of(new Item(30, "Item3", "Desc3", 8)));

            mvc.perform(get("/api/shops/all-open")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[0].id").value(10))
               .andExpect(jsonPath("$[1].id").value(11));
        }

        @Test
        void emptyList_returnsEmptyArray() throws Exception {
            when(shopService.getAllOpenShops("tok"))
                .thenReturn(List.of());
            when(shopService.getItems("tok"))
                .thenReturn(List.of());

            mvc.perform(get("/api/shops/all-open")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }

        @Test
        void exception_returnsEmptyList() throws Exception {
            when(shopService.getAllOpenShops("tok"))
                .thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/shops/all-open")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }
    }

    @Nested
    @DisplayName("Get All Closed Shops Tests")
    class GetAllClosedShopsTests {
        @Test
        void success_returnsShopList() throws Exception {
            Shop shop1 = new Shop(20, "Global Closed 1", stubShip());
            Shop shop2 = new Shop(21, "Global Closed 2", stubShip());
            when(shopService.getAllClosedShops("tok"))
                .thenReturn(List.of(shop1, shop2));
            when(shopService.getItems("tok"))
                .thenReturn(List.of(new Item(40, "Item4", "Desc4", 2)));

            mvc.perform(get("/api/shops/all-closed")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[0].id").value(20))
               .andExpect(jsonPath("$[1].id").value(21));
        }

        @Test
        void emptyList_returnsEmptyArray() throws Exception {
            when(shopService.getAllClosedShops("tok"))
                .thenReturn(List.of());
            when(shopService.getItems("tok"))
                .thenReturn(List.of());

            mvc.perform(get("/api/shops/all-closed")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }

        @Test
        void exception_returnsEmptyList() throws Exception {
            when(shopService.getAllClosedShops("tok"))
                .thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/shops/all-closed")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }
    }

    @Nested
    @DisplayName("Get All Shops Enhanced Tests")
    class GetAllShopsEnhancedTests {
        @Test
        void success_returnsShopList() throws Exception {
            Shop shop1 = new Shop(1, "Shop A", stubShip());
            Shop shop2 = new Shop(2, "Shop B", stubShip());
            when(shopService.getAllShops("tok"))
                .thenReturn(List.of(shop1, shop2));
            when(shopService.getItems("tok"))
                .thenReturn(List.of(new Item(1, "Item1", "Desc1", 5)));

            mvc.perform(get("/api/shops/all")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[0].id").value(1))
               .andExpect(jsonPath("$[1].id").value(2));
        }

        @Test
        void emptyList_returnsEmptyArray() throws Exception {
            when(shopService.getAllShops("tok"))
                .thenReturn(List.of());
            when(shopService.getItems("tok"))
                .thenReturn(List.of());

            mvc.perform(get("/api/shops/all")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }

        @Test
        void exception_returnsEmptyList() throws Exception {
            when(shopService.getAllShops("tok"))
                .thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/shops/all")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }

        @Test
        void illegalArg_returnsEmptyList() throws Exception {
            when(shopService.getAllShops("tok"))
                .thenThrow(new IllegalArgumentException("Invalid token"));

            mvc.perform(get("/api/shops/all")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }
    }

    @Nested
    @DisplayName("List Items By Shop Tests")
    class ListItemsByShopTests {
        @Test
        void success_returnsItemList() throws Exception {
            when(shopService.getItemsByShop(1, "tok"))
                .thenReturn(List.of(new Item(10, "Item A", "Description A", 5)));

            mvc.perform(get("/api/shops/1/items")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(1)))
               .andExpect(jsonPath("$[0].id").value(10))
               .andExpect(jsonPath("$[0].name").value("Item A"));
        }

        @Test
        void emptyList_returnsEmptyArray() throws Exception {
            when(shopService.getItemsByShop(2, "tok"))
                .thenReturn(List.of());

            mvc.perform(get("/api/shops/2/items")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }

        @Test
        void notFound_returns404() throws Exception {
            when(shopService.getItemsByShop(999, "tok"))
                .thenThrow(new NoSuchElementException("Shop not found"));

            mvc.perform(get("/api/shops/999/items")
                    .param("token", "tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void exception_returns500() throws Exception {
            when(shopService.getItemsByShop(3, "tok"))
                .thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/shops/3/items")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Search Items In Shop Tests")
    class SearchItemsInShopTests {
        @Test
        void success_returnsItemList() throws Exception {
            when(shopService.searchItemsInShop(eq(1), eq("TestItem"), eq(ItemCategory.ELECTRONICS), 
                    eq(null), eq(10), eq(100), eq(4.5), eq("tok")))
                .thenReturn(List.of(new Item(15, "TestItem", "Electronics item", 0)));

            mvc.perform(get("/api/shops/1/search")
                    .param("name", "TestItem")
                    .param("category", "ELECTRONICS")
                    .param("minPrice", "10")
                    .param("maxPrice", "100")
                    .param("minProductRating", "4.5")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(1)))
               .andExpect(jsonPath("$[0].id").value(15));
        }

        @Test
        void emptyResult_returnsEmptyArray() throws Exception {
            when(shopService.searchItemsInShop(2, null, null, null, null, null, null, "tok"))
                .thenReturn(List.of());

            mvc.perform(get("/api/shops/2/search")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }

        @Test
        void badRequest_missingToken() throws Exception {
            mvc.perform(get("/api/shops/1/search")
                    .param("name", "test"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void exception_returns500() throws Exception {
            when(shopService.searchItemsInShop(eq(4), any(), any(), any(), any(), any(), any(), eq("tok")))
                .thenThrow(new RuntimeException("Search failed"));

            mvc.perform(get("/api/shops/4/search")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Get Average Rating Tests")
    class GetAverageRatingTests {
        @Test
        void success_returnsRating() throws Exception {
            when(shopService.getShopAverageRating(1, "tok"))
                .thenReturn(4.5);

            mvc.perform(get("/api/shops/1/rating")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().string("4.5"));
        }

        @Test
        void zeroRating_returnsZero() throws Exception {
            when(shopService.getShopAverageRating(2, "tok"))
                .thenReturn(0.0);

            mvc.perform(get("/api/shops/2/rating")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().string("0.0"));
        }

        @Test
        void notFound_returns404() throws Exception {
            when(shopService.getShopAverageRating(999, "tok"))
                .thenThrow(new NoSuchElementException("Shop not found"));

            mvc.perform(get("/api/shops/999/rating")
                    .param("token", "tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void exception_returns500() throws Exception {
            when(shopService.getShopAverageRating(3, "tok"))
                .thenThrow(new RuntimeException("Rating calculation failed"));

            mvc.perform(get("/api/shops/3/rating")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("List All Items Tests")
    class ListAllItemsTests {
        @Test
        void success_returnsItemList() throws Exception {
            when(shopService.getItems("tok"))
                .thenReturn(List.of(
                    new Item(1, "Item1", "Desc1", 0),
                    new Item(2, "Item2", "Desc2", 1)
                ));

            mvc.perform(get("/api/shops/items")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[0].id").value(1))
               .andExpect(jsonPath("$[1].id").value(2));
        }

        @Test
        void emptyList_returnsEmptyArray() throws Exception {
            when(shopService.getItems("tok"))
                .thenReturn(List.of());

            mvc.perform(get("/api/shops/items")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }

        @Test
        void exception_returns500() throws Exception {
            when(shopService.getItems("tok"))
                .thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/shops/items")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Create Shop Enhanced Tests")
    class CreateShopEnhancedTests {
        @Test
        void success_returns201() throws Exception {
            when(shopService.createShop(eq("NewShop"), eq(null), any(), eq("tok")))
                .thenReturn(new Shop(100, "NewShop", stubShip()));

            mvc.perform(post("/api/shops/create")
                    .param("name", "NewShop")
                    .param("token", "tok"))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value(100))
               .andExpect(jsonPath("$.name").value("NewShop"));
        }

        @Test
        void illegalArg_returns400() throws Exception {
            when(shopService.createShop(eq(""), eq(null), any(), eq("tok")))
                .thenThrow(new IllegalArgumentException("Invalid name"));

            mvc.perform(post("/api/shops/create")
                    .param("name", "")
                    .param("token", "tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns409() throws Exception {
            when(shopService.createShop(eq("TestShop"), eq(null), any(), eq("invalidToken")))
                .thenThrow(new NoSuchElementException("User not found"));

            mvc.perform(post("/api/shops/create")
                    .param("name", "TestShop")
                    .param("token", "invalidToken"))
               .andExpect(status().isConflict());
        }

        @Test
        void conflict_returns409() throws Exception {
            when(shopService.createShop(eq("DuplicateShop"), eq(null), any(), eq("tok")))
                .thenThrow(new RuntimeException("Shop already exists"));

            mvc.perform(post("/api/shops/create")
                    .param("name", "DuplicateShop")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("Database error"); })
                .when(shopService).createShop(eq("ErrorShop"), eq(null), any(), eq("tok"));

            mvc.perform(post("/api/shops/create")
                    .param("name", "ErrorShop")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Get Shop Enhanced Tests")
    class GetShopEnhancedTests {
        @Test
        void success_returnsShop() throws Exception {
            when(shopService.getShop(1, "tok"))
                .thenReturn(new Shop(1, "TestShop", stubShip()));
            when(shopService.getItems("tok"))
                .thenReturn(List.of(new Item(5, "Item5", "Desc5", 2)));

            mvc.perform(get("/api/shops/1")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1))
               .andExpect(jsonPath("$.name").value("TestShop"));
        }

        @Test
        void notFound_returns404() throws Exception {
            when(shopService.getShop(999, "tok"))
                .thenThrow(new NoSuchElementException("Shop not found"));

            mvc.perform(get("/api/shops/999")
                    .param("token", "tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void illegalArg_returns409() throws Exception {
            when(shopService.getShop(-1, "tok"))
                .thenThrow(new IllegalArgumentException("Invalid shop ID"));

            mvc.perform(get("/api/shops/-1")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("Service error"); })
                .when(shopService).getShop(2, "tok");

            mvc.perform(get("/api/shops/2")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Get Discounts Enhanced Tests")
    class GetDiscountsEnhancedTests {
        @Test
        void success_returnsDiscountList() throws Exception {
            Discount discount1 = org.mockito.Mockito.mock(Discount.class);
            Discount discount2 = org.mockito.Mockito.mock(Discount.class);
            when(shopService.getDiscounts(1, "tok"))
                .thenReturn(List.of(discount1, discount2));

            mvc.perform(get("/api/shops/1/discounts")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void emptyList_returnsEmptyArray() throws Exception {
            when(shopService.getDiscounts(2, "tok"))
                .thenReturn(List.of());

            mvc.perform(get("/api/shops/2/discounts")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().json("[]"));
        }

        @Test
        void notFound_returns404() throws Exception {
            when(shopService.getDiscounts(999, "tok"))
                .thenThrow(new NoSuchElementException("Shop not found"));

            mvc.perform(get("/api/shops/999/discounts")
                    .param("token", "tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void illegalArg_returns409() throws Exception {
            when(shopService.getDiscounts(3, "tok"))
                .thenThrow(new IllegalArgumentException("Invalid parameters"));

            mvc.perform(get("/api/shops/3/discounts")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("Database error"); })
                .when(shopService).getDiscounts(4, "tok");

            mvc.perform(get("/api/shops/4/discounts")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Check Supply Tests")
    class CheckSupplyTests {
        @Test
        void available_returnsTrue() throws Exception {
            when(shopService.checkSupplyAvailability(1, 5, "tok"))
                .thenReturn(true);

            mvc.perform(get("/api/shops/1/items/5/available")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().string("true"));
        }

        @Test
        void notAvailable_returnsFalse() throws Exception {
            when(shopService.checkSupplyAvailability(1, 6, "tok"))
                .thenReturn(false);

            mvc.perform(get("/api/shops/1/items/6/available")
                    .param("token", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().string("false"));
        }

        @Test
        void exception_returns500() throws Exception {
            when(shopService.checkSupplyAvailability(1, 7, "tok"))
                .thenThrow(new RuntimeException("Supply check failed"));

            mvc.perform(get("/api/shops/1/items/7/available")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Add Supply Tests")
    class AddSupplyTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(shopService).addSupply(1, 5, 10, "tok");

            mvc.perform(patch("/api/shops/1/items/5/supply/add")
                    .param("supply", "10")
                    .param("token", "tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void exception_returns500() throws Exception {
            doThrow(new RuntimeException("Add supply failed"))
                .when(shopService).addSupply(1, 5, 10, "tok");

            mvc.perform(patch("/api/shops/1/items/5/supply/add")
                    .param("supply", "10")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Remove Supply Tests")
    class RemoveSupplyTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(shopService).removeSupply(1, 5, 5, "tok");

            mvc.perform(post("/api/shops/1/items/5/supply/remove")
                    .param("supply", "5")
                    .param("token", "tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void exception_returns500() throws Exception {
            doThrow(new RuntimeException("Remove supply failed"))
                .when(shopService).removeSupply(1, 5, 5, "tok");

            mvc.perform(post("/api/shops/1/items/5/supply/remove")
                    .param("supply", "5")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Update Policy Enhanced Tests")
    class UpdatePolicyEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(shopService).updatePurchasePolicy(1, null, "tok");

            mvc.perform(post("/api/shops/1/policy")
                    .param("token", "tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void illegalArg_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid policy"))
                .when(shopService).updatePurchasePolicy(1, null, "tok");

            mvc.perform(post("/api/shops/1/policy")
                    .param("token", "tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException("Shop not found"))
                .when(shopService).updatePurchasePolicy(999, null, "tok");

            mvc.perform(post("/api/shops/999/policy")
                    .param("token", "tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException("Policy conflict"))
                .when(shopService).updatePurchasePolicy(2, null, "tok");

            mvc.perform(post("/api/shops/2/policy")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("Database error"); })
                .when(shopService).updatePurchasePolicy(3, null, "tok");

            mvc.perform(post("/api/shops/3/policy")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Set Discount Policy Enhanced Tests")
    class SetDiscountPolicyEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(shopService).setDiscountPolicy(eq(1), any(), eq("tok"));

            mvc.perform(post("/api/shops/1/discount/policy")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
               .andExpect(status().isNoContent());
        }

        @Test
        void illegalArg_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid policy"))
                .when(shopService).setDiscountPolicy(anyInt(), any(), anyString());

            mvc.perform(post("/api/shops/1/discount/policy")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void constraintViolation_returns400() throws Exception {
            doThrow(new ConstraintViolationException("Validation failed", null))
                .when(shopService).setDiscountPolicy(anyInt(), any(), anyString());

            mvc.perform(post("/api/shops/1/discount/policy")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException("Shop not found"))
                .when(shopService).setDiscountPolicy(anyInt(), any(), anyString());

            mvc.perform(post("/api/shops/999/discount/policy")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
               .andExpect(status().isNotFound());
        }

        @Test
        void runtimeException_returns409() throws Exception {
            doThrow(new RuntimeException("Policy conflict"))
                .when(shopService).setDiscountPolicy(anyInt(), any(), anyString());

            mvc.perform(post("/api/shops/1/discount/policy")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("Database error"); })
                .when(shopService).setDiscountPolicy(anyInt(), any(), anyString());

            mvc.perform(post("/api/shops/1/discount/policy")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Add Supply To Item Tests")
    class AddSupplyToItemTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(shopService).addSupplyToItem(1, 5, 15, "tok");

            mvc.perform(post("/api/shops/1/items/5/supply")
                    .param("quantity", "15")
                    .param("token", "tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void exception_returns500() throws Exception {
            doThrow(new RuntimeException("Add supply to item failed"))
                .when(shopService).addSupplyToItem(1, 5, 15, "tok");

            mvc.perform(post("/api/shops/1/items/5/supply")
                    .param("quantity", "15")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Check Policy Tests")
    class CheckPolicyTests {
        @Test
        void policyPass_returnsTrue() throws Exception {
            when(shopService.checkPolicy(any(), eq("tok")))
                .thenReturn(true);

            mvc.perform(post("/api/shops/policy/check")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
               .andExpect(status().isOk())
               .andExpect(content().string("true"));
        }

        @Test
        void policyFail_returnsFalse() throws Exception {
            when(shopService.checkPolicy(any(), eq("tok")))
                .thenReturn(false);

            mvc.perform(post("/api/shops/policy/check")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
               .andExpect(status().isOk())
               .andExpect(content().string("false"));
        }

        @Test
        void exception_returns500() throws Exception {
            when(shopService.checkPolicy(any(), eq("tok")))
                .thenThrow(new RuntimeException("Policy check failed"));

            mvc.perform(post("/api/shops/policy/check")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Close Shop Enhanced Tests")
    class CloseShopEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(shopService).closeShop(1, "tok");

            mvc.perform(delete("/api/shops/1")
                    .param("token", "tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException("Shop not found"))
                .when(shopService).closeShop(999, "tok");

            mvc.perform(delete("/api/shops/999")
                    .param("token", "tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException("Cannot close shop"))
                .when(shopService).closeShop(2, "tok");

            mvc.perform(delete("/api/shops/2")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("Database error"); })
                .when(shopService).closeShop(3, "tok");

            mvc.perform(delete("/api/shops/3")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Add Review Enhanced Tests")
    class AddReviewEnhancedTests {
        @Test
        void success_returns202() throws Exception {
            doNothing().when(shopService).addReviewToShop(1, 5, "Excellent!", "tok");

            mvc.perform(post("/api/shops/1/reviews")
                    .param("rating", "5")
                    .param("reviewText", "Excellent!")
                    .param("token", "tok"))
               .andExpect(status().isAccepted());
        }

        @Test
        void illegalArg_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid rating"))
                .when(shopService).addReviewToShop(1, 6, "Too high", "tok");

            mvc.perform(post("/api/shops/1/reviews")
                    .param("rating", "6")
                    .param("reviewText", "Too high")
                    .param("token", "tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException("Shop not found"))
                .when(shopService).addReviewToShop(999, 4, "Good", "tok");

            mvc.perform(post("/api/shops/999/reviews")
                    .param("rating", "4")
                    .param("reviewText", "Good")
                    .param("token", "tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException("Review already exists"))
                .when(shopService).addReviewToShop(2, 3, "Average", "tok");

            mvc.perform(post("/api/shops/2/reviews")
                    .param("rating", "3")
                    .param("reviewText", "Average")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("Database error"); })
                .when(shopService).addReviewToShop(3, 4, "Nice", "tok");

            mvc.perform(post("/api/shops/3/reviews")
                    .param("rating", "4")
                    .param("reviewText", "Nice")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Remove Item Enhanced Tests")
    class RemoveItemEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(shopService).removeItemFromShop(1, 5, "tok");

            mvc.perform(delete("/api/shops/1/items/5")
                    .param("token", "tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void illegalArg_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid item"))
                .when(shopService).removeItemFromShop(1, -1, "tok");

            mvc.perform(delete("/api/shops/1/items/-1")
                    .param("token", "tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException("Item not found"))
                .when(shopService).removeItemFromShop(1, 999, "tok");

            mvc.perform(delete("/api/shops/1/items/999")
                    .param("token", "tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException("Cannot remove item"))
                .when(shopService).removeItemFromShop(2, 5, "tok");

            mvc.perform(delete("/api/shops/2/items/5")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("Database error"); })
                .when(shopService).removeItemFromShop(3, 5, "tok");

            mvc.perform(delete("/api/shops/3/items/5")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Remove Item Discount Enhanced Tests")
    class RemoveItemDiscountEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(shopService).removeDiscountForItem(1, 5, "tok");

            mvc.perform(delete("/api/shops/1/discount/items/5")
                    .param("token", "tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void illegalArg_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid discount"))
                .when(shopService).removeDiscountForItem(1, 5, "tok");

            mvc.perform(delete("/api/shops/1/discount/items/5")
                    .param("token", "tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException("Discount not found"))
                .when(shopService).removeDiscountForItem(1, 999, "tok");

            mvc.perform(delete("/api/shops/1/discount/items/999")
                    .param("token", "tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException("Cannot remove discount"))
                .when(shopService).removeDiscountForItem(2, 5, "tok");

            mvc.perform(delete("/api/shops/2/discount/items/5")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("Database error"); })
                .when(shopService).removeDiscountForItem(3, 5, "tok");

            mvc.perform(delete("/api/shops/3/discount/items/5")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Add Item Enhanced Tests")
    class AddItemEnhancedTests {
        @Test
        void success_returns201() throws Exception {
            doNothing().when(shopService).addItemToShop(1, "NewItem", "Description", 
                    10, ItemCategory.ELECTRONICS, 150, "tok");

            mvc.perform(post("/api/shops/1/items")
                    .param("name", "NewItem")
                    .param("description", "Description")
                    .param("quantity", "10")
                    .param("category", "ELECTRONICS")
                    .param("price", "150")
                    .param("token", "tok"))
               .andExpect(status().isCreated());
        }

        @Test
        void illegalArg_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid item data"))
                .when(shopService).addItemToShop(1, "", "Desc", 5, ItemCategory.BOOKS, 50, "tok");

            mvc.perform(post("/api/shops/1/items")
                    .param("name", "")
                    .param("description", "Desc")
                    .param("quantity", "5")
                    .param("category", "BOOKS")
                    .param("price", "50")
                    .param("token", "tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns409() throws Exception {
            doThrow(new NoSuchElementException("Shop not found"))
                .when(shopService).addItemToShop(999, "Item", "Desc", 5, ItemCategory.TOYS, 25, "tok");

            mvc.perform(post("/api/shops/999/items")
                    .param("name", "Item")
                    .param("description", "Desc")
                    .param("quantity", "5")
                    .param("category", "TOYS")
                    .param("price", "25")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException("Item already exists"))
                .when(shopService).addItemToShop(2, "DuplicateItem", "Desc", 3, ItemCategory.CLOTHING, 75, "tok");

            mvc.perform(post("/api/shops/2/items")
                    .param("name", "DuplicateItem")
                    .param("description", "Desc")
                    .param("quantity", "3")
                    .param("category", "CLOTHING")
                    .param("price", "75")
                    .param("token", "tok"))
               .andExpect(status().isConflict());
        }

        @Test
        void serverError_returns500() throws Exception {
            doAnswer(invocation -> { throw new IOException("Database error"); })
                .when(shopService).addItemToShop(3, "ErrorItem", "Desc", 2, ItemCategory.GROCERY, 10, "tok");

            mvc.perform(post("/api/shops/3/items")
                    .param("name", "ErrorItem")
                    .param("description", "Desc")
                    .param("quantity", "2")
                    .param("category", "GROCERY")
                    .param("price", "10")
                    .param("token", "tok"))
               .andExpect(status().isInternalServerError());
        }
    }
}
