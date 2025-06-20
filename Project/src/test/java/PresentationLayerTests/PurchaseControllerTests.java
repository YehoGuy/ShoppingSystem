package PresentationLayerTests;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.ApplicationLayer.Purchase.PurchaseService;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.PresentationLayer.Controller.PurchaseController;
import com.example.app.PresentationLayer.DTO.Purchase.PaymentDetailsDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Comprehensive slice tests for PurchaseController.
 */
@WebMvcTest(controllers = PurchaseController.class)
@ContextConfiguration(classes = PurchaseControllerTests.TestBootApp.class)
@AutoConfigureMockMvc(addFilters = false)
class PurchaseControllerTests {

        @SpringBootApplication(scanBasePackages = "com.example.app.PresentationLayer")
        static class TestBootApp {
        }

        @Autowired
        private MockMvc mvc;

        @MockBean
        private PurchaseService purchaseService;

        @Nested
        @DisplayName("1. CHECKOUT")
        class CheckoutTests {
                PaymentDetailsDTO paymentDetails;
                String paymentJson;

                @BeforeEach
                void setup() throws JsonProcessingException {
                        paymentDetails = new PaymentDetailsDTO("USD", "1234567890123456", "12", "25", "John Doe", "123",
                                        "tok");
                        paymentJson = new ObjectMapper().writeValueAsString(paymentDetails);
                }

                @Test
                void success_returns201() throws Exception {
                        // stub service
                        when(purchaseService.checkoutCart(eq("tok"), any(Address.class), anyString(), anyString(),
                                        anyString(), anyString(), anyString(), anyString(), anyString()))
                                        .thenReturn(List.of(100, 101));

                        mvc.perform(post("/api/purchases/checkout")
                                        .param("authToken", "tok")
                                        .param("country", "US")
                                        .param("city", "NY")
                                        .param("street", "Broadway")
                                        .param("houseNumber", "1")
                                        .param("zipCode", "1234567")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(paymentJson))
                                        .andExpect(status().isCreated())
                                        .andExpect(content().json("[100,101]"));
                }

                @Test
                void badRequest_returns400() throws Exception {
                        when(purchaseService.checkoutCart(anyString(), any(), anyString(), anyString(), anyString(),
                                        anyString(), anyString(), anyString(), anyString()))
                                        .thenThrow(new IllegalArgumentException());

                        mvc.perform(post("/api/purchases/checkout")
                                        .param("authToken", "tok")
                                        .param("country", "US")
                                        .param("city", "NY")
                                        .param("street", "Broadway")
                                        .param("houseNumber", "1")
                                        .param("zipCode", "1234567")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(paymentJson))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                void notFound_returns404() throws Exception {
                        when(purchaseService.checkoutCart(anyString(), any(), anyString(), anyString(), anyString(),
                                        anyString(), anyString(), anyString(), anyString()))
                                        .thenThrow(new NoSuchElementException());

                        mvc.perform(post("/api/purchases/checkout")
                                        .param("authToken", "tok")
                                        .param("country", "US")
                                        .param("city", "NY")
                                        .param("street", "Broadway")
                                        .param("houseNumber", "1")
                                        .param("zipCode", "1234567")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(paymentJson))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void conflict_returns409() throws Exception {
                        when(purchaseService.checkoutCart(anyString(), any(), anyString(), anyString(), anyString(),
                                        anyString(), anyString(), anyString(), anyString()))
                                        .thenThrow(new RuntimeException());

                        mvc.perform(post("/api/purchases/checkout")
                                        .param("authToken", "tok")
                                        .param("country", "US")
                                        .param("city", "NY")
                                        .param("street", "Broadway")
                                        .param("houseNumber", "1")
                                        .param("zipCode", "1234567")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(paymentJson))
                                        .andExpect(status().isConflict());
                }
        }

        @Nested
        @DisplayName("2. CREATE BID")
        class CreateBidTests {
                @Test
                void success_returns201() throws Exception {
                        when(purchaseService.createBid(eq("tok"), eq(5), anyMap(), eq(50)))
                                        .thenReturn(200);

                        mvc.perform(post("/api/purchases/bids")
                                        .param("authToken", "tok")
                                        .param("storeId", "5")
                                        .param("initialPrice", "50")
                                        .contentType("application/json")
                                        .content("{\"1\":2}"))
                                        .andExpect(status().isCreated())
                                        .andExpect(content().string("200"));
                }

                @Test
                void badRequest_returns400() throws Exception {
                        when(purchaseService.createBid(anyString(), anyInt(), anyMap(), anyInt()))
                                        .thenThrow(new IllegalArgumentException());

                        mvc.perform(post("/api/purchases/bids")
                                        .param("authToken", "tok")
                                        .param("storeId", "5")
                                        .param("initialPrice", "50")
                                        .contentType("application/json")
                                        .content("{\"1\":2}"))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                void notFound_returns404() throws Exception {
                        when(purchaseService.createBid(anyString(), anyInt(), anyMap(), anyInt()))
                                        .thenThrow(new NoSuchElementException());

                        mvc.perform(post("/api/purchases/bids")
                                        .param("authToken", "tok")
                                        .param("storeId", "5")
                                        .param("initialPrice", "50")
                                        .contentType("application/json")
                                        .content("{\"1\":2}"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void conflict_returns409() throws Exception {
                        when(purchaseService.createBid(anyString(), anyInt(), anyMap(), anyInt()))
                                        .thenThrow(new RuntimeException());

                        mvc.perform(post("/api/purchases/bids")
                                        .param("authToken", "tok")
                                        .param("storeId", "5")
                                        .param("initialPrice", "50")
                                        .contentType("application/json")

                                        .content("{\"1\":2}"))
                                        .andExpect(status().isConflict());
                }
        }

        @Nested
        @DisplayName("3. POST BID OFFER")
        class PostBidOfferTests {
                @Test
                void success_returns202() throws Exception {
                        doNothing().when(purchaseService).postBidding(eq("tok"), eq(10), eq(75));

                        mvc.perform(post("/api/purchases/bids/10/offers")
                                        .param("authToken", "tok")
                                        .param("bidPrice", "75"))
                                        .andExpect(status().isAccepted());
                }

                @Test
                void badRequest_returns400() throws Exception {
                        doNothing().when(purchaseService).postBidding(eq("tok"), eq(10), eq(75));
                        // invalid bidAmount param missing / negative
                        mvc.perform(post("/api/purchases/bids/10/offers")
                                        .param("authToken", "tok"))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                void conflict_returns409() throws Exception {
                        doNothing().when(purchaseService).postBidding(anyString(), anyInt(), anyInt());
                        doThrow(new RuntimeException()).when(purchaseService).postBidding(eq("tok"), eq(10), eq(75));

                        mvc.perform(post("/api/purchases/bids/10/offers")
                                        .param("authToken", "tok")
                                        .param("bidPrice", "75"))
                                        .andExpect(status().isConflict());
                }
        }

        @Nested
        @DisplayName("4. FINALIZE BID")
        class FinalizeBidTests {
                @Test
                void success_returns200() throws Exception {
                        when(purchaseService.finalizeBid(eq("tok"), eq(10), eq(false))).thenReturn(999);

                        String paymentJson = "{\"currency\":\"USD\",\"cardNumber\":\"1234567890123456\",\"expirationDateMonth\":\"12\",\"expirationDateYear\":\"25\",\"cardHolderName\":\"John Doe\",\"cvv\":\"123\",\"id\":\"tok\"}";

                        mvc.perform(post("/api/purchases/bids/10/finalize")
                                        .param("authToken", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("999"));
                }

                @Test
                void badRequest_returns400() throws Exception {
                        when(purchaseService.finalizeBid(anyString(), anyInt(), eq(false)))
                                        .thenThrow(new IllegalArgumentException());

                        mvc.perform(post("/api/purchases/bids/10/finalize")
                                        .param("authToken", "tok"))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                void notFound_returns404() throws Exception {
                        when(purchaseService.finalizeBid(anyString(), anyInt(), eq(false)))
                                        .thenThrow(new NoSuchElementException());

                        mvc.perform(post("/api/purchases/bids/10/finalize")
                                        .param("authToken", "tok"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void conflict_returns409() throws Exception {
                        when(purchaseService.finalizeBid(anyString(), anyInt(), eq(false)))
                                        .thenThrow(new RuntimeException());

                        mvc.perform(post("/api/purchases/bids/10/finalize")
                                        .param("authToken", "tok"))
                                        .andExpect(status().isConflict());
                }
        }

        @Nested
        @DisplayName("5. USER PURCHASE HISTORY")
        class UserPurchaseHistoryTests {

                @Test
                void success_returns200AndJsonList() throws Exception {
                        // stub service – empty list is enough to prove mapping
                        when(purchaseService.getUserPurchases("tok", 7))
                                        .thenReturn(List.of());

                        mvc.perform(get("/api/purchases/users/7")
                                        .param("authToken", "tok"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().json("[]"));
                }

                @Test
                void badRequest_returns400() throws Exception {
                        when(purchaseService.getUserPurchases(anyString(), anyInt()))
                                        .thenThrow(new IllegalArgumentException());

                        mvc.perform(get("/api/purchases/users/3")
                                        .param("authToken", "tok"))
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("POST /api/purchases/auctions")
        class StartAuctionTests {
        private final String BASE = "/api/purchases/auctions";
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Test
        @DisplayName("whenValid_returns201_andAuctionId")
        void validStartAuction_returns201() throws Exception {
                Map<Integer,Integer> items = Map.of(1, 2);
                when(purchaseService.startAuction(
                        anyString(), anyInt(), anyMap(), anyInt(), any(LocalDateTime.class))
                ).thenReturn(77);

                String json = objectMapper.writeValueAsString(items);
                mvc.perform(post(BASE)
                        .param("authToken",    "tok")
                        .param("storeId",      "5")
                        .param("initialPrice", "100")
                        .param("auctionEndTime","2025-06-21T12:00:00")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(content().string("77"));
        }

        @Test
        @DisplayName("whenBadArgs_returns400")
        void badArgs_returns400() throws Exception {
                when(purchaseService.startAuction(
                        anyString(), anyInt(), anyMap(), anyInt(), any(LocalDateTime.class))
                ).thenThrow(new IllegalArgumentException());

                mvc.perform(post(BASE)
                        .param("authToken","tok")
                        .param("storeId","-1")
                        .param("initialPrice","100")
                        .param("auctionEndTime","2025-06-21T12:00:00")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                )
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("whenStoreNotFound_returns404")
        void storeNotFound_returns404() throws Exception {
                when(purchaseService.startAuction(
                        anyString(), anyInt(), anyMap(), anyInt(), any(LocalDateTime.class))
                ).thenThrow(new NoSuchElementException());

                mvc.perform(post(BASE)
                        .param("authToken","tok")
                        .param("storeId","999")
                        .param("initialPrice","100")
                        .param("auctionEndTime","2025-06-21T12:00:00")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                )
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("whenConflict_returns409")
        void conflict_returns409() throws Exception {
                when(purchaseService.startAuction(
                        anyString(), anyInt(), anyMap(), anyInt(), any(LocalDateTime.class))
                ).thenThrow(new RuntimeException());

                mvc.perform(post(BASE)
                        .param("authToken","tok")
                        .param("storeId","5")
                        .param("initialPrice","100")
                        .param("auctionEndTime","2025-06-21T12:00:00")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                )
                .andExpect(status().isConflict());
        }
        }

        @Nested
        @DisplayName("POST /api/purchases/auctions/{auctionId}/finalize")
        class FinalizeAuctionTests {
        private final String BASE = "/api/purchases/auctions";

        @Test
        @DisplayName("whenSuccess_returns200_andWinnerId")
        void success_returns200() throws Exception {
                when(purchaseService.finalizeBid(anyString(), anyInt(), eq(false)))
                .thenReturn(42);

                mvc.perform(post(BASE + "/10/finalize")
                        .param("authToken", "tok")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
        }

        @Test
        @DisplayName("whenBadArgs_returns400")
        void badArgs_returns400() throws Exception {
                when(purchaseService.finalizeBid(anyString(), anyInt(), eq(false)))
                .thenThrow(new IllegalArgumentException("bad"));

                mvc.perform(post(BASE + "/0/finalize")
                        .param("authToken", "tok")
                )
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("whenNotFound_returns404")
        void notFound_returns404() throws Exception {
                when(purchaseService.finalizeBid(anyString(), anyInt(), eq(false)))
                .thenThrow(new NoSuchElementException());

                mvc.perform(post(BASE + "/99/finalize")
                        .param("authToken", "tok")
                )
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("whenConflict_returns409")
        void conflict_returns409() throws Exception {
                when(purchaseService.finalizeBid(anyString(), anyInt(), eq(false)))
                .thenThrow(new RuntimeException());

                mvc.perform(post(BASE + "/10/finalize")
                        .param("authToken", "tok")
                )
                .andExpect(status().isConflict());
        }
        }

}
