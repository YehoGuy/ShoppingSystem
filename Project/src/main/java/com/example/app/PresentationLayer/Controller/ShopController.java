package com.example.app.PresentationLayer.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.ApplicationLayer.Shop.ShopService;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Operator;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.DomainLayer.Shop.Discount.Discount;
import com.example.app.PresentationLayer.DTO.Item.ItemDTO;
import com.example.app.PresentationLayer.DTO.Shop.CompositePolicyDTO;
import com.example.app.PresentationLayer.DTO.Shop.DiscountDTO;
import com.example.app.PresentationLayer.DTO.Shop.PoliciesDTO;
import com.example.app.PresentationLayer.DTO.Shop.ShopDTO;
import com.example.app.DomainLayer.Shop.Discount.SingleDiscount;
import com.example.app.DomainLayer.Shop.Discount.CategoryDiscount;
import com.example.app.DomainLayer.Shop.Discount.GlobalDiscount;
import com.example.app.DomainLayer.Shop.Discount.Policy;
import com.example.app.DomainLayer.Shop.Discount.PolicyComposite;
import com.example.app.DomainLayer.Shop.Discount.PolicyLeaf;

import jakarta.validation.ConstraintViolationException;

/**
 * Base path : /api/shops     (JSON in / JSON out)
 *
 * ─────────────────────────────── SHOP LIFE‑CYCLE
 * ────────────────────────────────
 *  1. POST /create params: token, name
 * → 201  ShopDTO
 *  2. GET /{shopId} params: token
 * → 200  ShopDTO
 *  3. GET /all params: token
 * → 200  [List<ShopDTO>]
 *  4. DELETE /{shopId} params: token
 * → 204
 *
 * ────────────────────────── SHOP‑LEVEL POLICY & VALIDATION
 * ───────────────────────
 *  5. POST /{shopId}/policy params: token, body {rule…}
 * → 204
 *  6. POST /policy/check params: token, body cart{shop→item→qty}
 * → 200  boolean
 *
 * ───────────────────────────── GLOBAL DISCOUNT RULES
 * ─────────────────────────────
 *  7. POST /{shopId}/discount/global params: token, discount, isDouble
 * → 204
 *  8. DELETE /{shopId}/discount/global params: token
 * → 204
 *
 * ─────────────────────────── ITEM‑SPECIFIC DISCOUNTS
 * ────────────────────────────
 *  9. POST /{shopId}/discount/items/{itemId} params: token, discount, isDouble
 * → 204
 * 10. DELETE /{shopId}/discount/items/{itemId} params: token
 * → 204
 *
 * ────────────────────────── CATEGORY‑LEVEL DISCOUNTS
 * ────────────────────────────
 * 11. POST /{shopId}/discount/categories params: token, category, discount,
 * isDouble
 * → 204
 * 12. DELETE /{shopId}/discount/categories params: token, category
 * → 204
 *
 * ───────────────────────────── REVIEWS & RATING
 * ─────────────────────────────────
 * 13. POST /{shopId}/reviews params: token, rating, reviewText
 * → 202
 * 14. GET /{shopId}/rating params: token
 * → 200  double
 *
 * ───────────────────────────── INVENTORY MANAGEMENT
 * ─────────────────────────────
 * 15. POST /{shopId}/items params: token, name, description, qty, price
 * → 201
 * 16. PATCH /{shopId}/items/{itemId}/price params: token, price
 * → 204
 * 17. DELETE /{shopId}/items/{itemId} params: token
 * → 204
 * 18. GET /{shopId}/items params: token
 * → 200  [List<ItemDTO>]
 * 19. GET /items params: token
 * → 200  [List<ItemDTO>]
 *
 * ───────────────────────────────── SEARCH ENDPOINTS
 * ─────────────────────────────
 * 20. GET /search params: token,
 * name|category|keywords|minPrice|maxPrice|minProductRating|minShopRating
 * → 200  [List<ItemDTO>]
 * 21. GET /{shopId}/search params: token,
 * name|category|keywords|minPrice|maxPrice|minProductRating
 * → 200  [List<ItemDTO>]
 * 
 * 28. GET /shops/ByWorkerId params: token, workerId
 *
 * ───────────────────────────── SUPPLY / STOCK CONTROL
 * ───────────────────────────
 * 22. POST /{shopId}/items/{itemId}/supply params: token, quantity
 * → 204  (add initial supply)
 * 23. GET /{shopId}/items/{itemId}/quantity params: token
 * → 200  int
 * 24. GET /{shopId}/items/{itemId}/available params: token
 * → 200  boolean
 * 25. PATCH /{shopId}/items/{itemId}/supply/add params: token, supply
 * → 204  (increase)
 * 26. PATCH /{shopId}/items/{itemId}/supply/remove params: token, supply
 * → 204  (decrease)
 *
 * ────────────────────────────── PURCHASE WORK‑FLOW
 * ───────────────────────────────
 * 27. POST /{shopId}/purchase/{purchaseId}/ship params: token, country, city,
 * street, postalCode
 * → 202
 * 
 * 
 * 
 *
 * ───────────────────────────── ERROR MAPPING (ALL)
 * ───────────────────────────────
 * 400 – Bad input / validation failure
 * 404 – Entity not found
 * 409 – Business‑rule conflict (permissions, stock, etc.)
 * 500 – Internal server error
 */
@RestController
@RequestMapping("/api/shops")
@Validated
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    /* ───────────────────────── 1. CREATE SHOP ───────────────────────── */

    @PostMapping("/create")
    public ResponseEntity<?> createShop(
            @RequestParam String token,
            @RequestParam String name) {

        try {
            Shop shop = shopService.createShop(name, null, null, token);
            List<ItemDTO> itemDTOs = new ArrayList<>(); // No items initially
            ShopDTO shopDTO = ShopDTO.fromDomain(shop, itemDTOs);
            return ResponseEntity.status(HttpStatus.CREATED).body(shopDTO);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }

    /* ───────────────────────── 2. GET ONE SHOP ──────────────────────── */

    @GetMapping("/{shopId}")
    public ResponseEntity<?> getShop(
            @PathVariable String shopId,
            @RequestParam String token) {

        try {
            int id = Integer.parseInt(shopId);
            if (id <= 0) {
                throw new IllegalArgumentException("Shop ID must be a positive integer.");
            }
            Shop shop = shopService.getShop(id, token);
            List<Item> items = shopService.getItemsByShop(id, token);
            List<ItemDTO> itemDTOs = items.stream()
                    .map(ItemDTO::fromDomain)
                    .toList();
            ShopDTO shopDTO = ShopDTO.fromDomain(shop, itemDTOs);
            return ResponseEntity.ok(shopDTO);

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    /* ───────────────────────── 3. GET ALL SHOPS ─────────────────────── */

    @GetMapping("/all")
    public ResponseEntity<List<ShopDTO>> getAllShops(@RequestParam String token) {
        try {
            List<Shop> shops = shopService.getAllShops(token);
            List<Item> items = shopService.getItems(token);
            List<ItemDTO> itemDTOs = items.stream()
                    .map(ItemDTO::fromDomain)
                    .toList();
            List<ShopDTO> shopDTOs = new ArrayList<>();
            for (Shop shop : shops) {
                shopDTOs.add(ShopDTO.fromDomain(shop, itemDTOs));
            }
            return ResponseEntity.ok(shopDTOs);
        } catch (Exception ex) {
            ex.printStackTrace(); // log the real error
            // Instead of a 500, return an empty list
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /* ───────────────────────── 4. DISCOUNTS & POLICY ────────────────── */

    @PostMapping("/{shopId}/policy")
    public ResponseEntity<?> updatePolicy(
            @PathVariable int shopId,
            @RequestParam String token) {
        try {
            // TODO: with Noam&Dor accept real policy params
            shopService.updatePurchasePolicy(shopId, null, token);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | ConstraintViolationException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* Global discount endpoints */
    @PostMapping("/{shopId}/discount/global")
    public ResponseEntity<?> setGlobalDiscount(
            @PathVariable int shopId,
            @RequestParam int discount,
            @RequestParam boolean isDouble,
            @RequestParam String token) {
        try {
            shopService.setGlobalDiscount(shopId, discount, isDouble, token);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{shopId}/discount/global")
    public ResponseEntity<?> removeGlobalDiscount(
            @PathVariable int shopId,
            @RequestParam String token) {
        try {
            shopService.removeGlobalDiscount(shopId, token);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* Item discount endpoints (similar pattern) */
    @PostMapping("/{shopId}/discount/items/{itemId}")
    public ResponseEntity<?> setItemDiscount(
            @PathVariable int shopId,
            @PathVariable int itemId,
            @RequestParam int discount,
            @RequestParam boolean isDouble,
            @RequestParam String token) {
        try {
            shopService.setDiscountForItem(shopId, itemId, discount, isDouble, token);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{shopId}/discount/items/{itemId}")
    public ResponseEntity<?> removeItemDiscount(
            @PathVariable int shopId,
            @PathVariable int itemId,
            @RequestParam String token) {
        try {
            shopService.removeDiscountForItem(shopId, itemId, token);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* Category discount endpoints */
    @PostMapping("/{shopId}/discount/categories")
    public ResponseEntity<?> setCategoryDiscount(
            @PathVariable int shopId,
            @RequestParam ItemCategory category,
            @RequestParam int discount,
            @RequestParam boolean isDouble,
            @RequestParam String token) {
        try {
            shopService.setCategoryDiscount(shopId, category, discount, isDouble, token);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{shopId}/discount/categories")
    public ResponseEntity<?> removeCategoryDiscount(
            @PathVariable int shopId,
            @RequestParam ItemCategory category,
            @RequestParam String token) {
        try {
            shopService.removeCategoryDiscount(shopId, category, token);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ───────────────────────── 5. REVIEWS ──────────────────────────── */

    @PostMapping("/{shopId}/reviews")
    public ResponseEntity<?> addReview(
            @PathVariable int shopId,
            @RequestParam int rating,
            @RequestParam String reviewText,
            @RequestParam String token) {
        try {
            shopService.addReviewToShop(shopId, rating, reviewText, token);
            return ResponseEntity.accepted().build(); // 202
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{shopId}/rating")
    public ResponseEntity<?> getAverageRating(
            @PathVariable int shopId,
            @RequestParam String token) {
        try {
            double avg = shopService.getShopAverageRating(shopId, token);
            return ResponseEntity.ok(avg);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ───────────────────────── 6. ITEM MANAGEMENT ─────────────────── */

    @PostMapping("/{shopId}/items")
    public ResponseEntity<?> addItem(
            @PathVariable int shopId,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam int quantity,
            @RequestParam ItemCategory category,
            @RequestParam int price,
            @RequestParam String token) {
        try {
            shopService.addItemToShop(shopId, name, description, quantity, category, price, token);
            return ResponseEntity.status(HttpStatus.CREATED).build(); // 201
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{shopId}/items/{itemId}/price")
    public ResponseEntity<?> updateItemPrice(
            @PathVariable int shopId,
            @PathVariable int itemId,
            @RequestParam int price,
            @RequestParam String token) {
        try {
            shopService.updateItemPriceInShop(shopId, itemId, price, token);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{shopId}/items/{itemId}")
    public ResponseEntity<?> removeItem(
            @PathVariable int shopId,
            @PathVariable int itemId,
            @RequestParam String token) {
        try {
            shopService.removeItemFromShop(shopId, itemId, token);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ───────────────────────── 7. SEARCH & INVENTORY ───────────────── */

    @GetMapping("/{shopId}/items")
    public ResponseEntity<?> listItemsByShop(
            @PathVariable int shopId,
            @RequestParam String token) {
        try {
            List<Item> items = shopService.getItemsByShop(shopId, token);
            List<ItemDTO> itemDTOs = items.stream()
                    .map(ItemDTO::fromDomain)
                    .toList();
            return ResponseEntity.ok(itemDTOs);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/items")
    public ResponseEntity<?> listAllItems(@RequestParam String token) {
        try {
            List<Item> items = shopService.getItems(token);
            List<ItemDTO> itemDTOs = items.stream()
                    .map(ItemDTO::fromDomain)
                    .toList();
            return ResponseEntity.ok(itemDTOs);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* General search across shops */
    @GetMapping("/search")
    public ResponseEntity<?> searchItems(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Double minProductRating,
            @RequestParam(required = false) Double minShopRating,
            @RequestParam String token) {
        try {
            List<Item> items = shopService.searchItems(
                    name, category, keywords, minPrice, maxPrice,
                    minProductRating, minShopRating, token);
            List<ItemDTO> itemDTOs = items.stream()
                    .map(ItemDTO::fromDomain)
                    .toList();
            return ResponseEntity.ok(itemDTOs);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* Search within one shop */
    @GetMapping("/{shopId}/search")
    public ResponseEntity<?> searchItemsInShop(
            @PathVariable int shopId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Double minProductRating,
            @RequestParam String token) {
        try {
            List<Item> items = shopService.searchItemsInShop(
                    shopId, name, category, keywords,
                    minPrice, maxPrice, minProductRating, token);
            List<ItemDTO> itemDTOs = items.stream()
                    .map(ItemDTO::fromDomain)
                    .toList();
            return ResponseEntity.ok(itemDTOs);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ───────────────────────── 8. CLOSE SHOP ───────────────────────── */

    @DeleteMapping("/{shopId}")
    public ResponseEntity<?> closeShop(
            @PathVariable int shopId,
            @RequestParam String token) {
        try {
            shopService.closeShop(shopId, token);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // adding from here
    @PostMapping("/{shopId}/items/{itemId}/supply")
    public ResponseEntity<?> addSupplyToItem(
            @PathVariable int shopId,
            @PathVariable int itemId,
            @RequestParam int quantity,
            @RequestParam String token) {
        try {
            shopService.addSupplyToItem(shopId, itemId, quantity, token);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @GetMapping("/{shopId}/items/{itemId}/quantity")
    public ResponseEntity<?> getItemQuantity(
            @PathVariable int shopId,
            @PathVariable int itemId,
            @RequestParam String token) {
        try {
            int qty = shopService.getItemQuantityFromShop(shopId, itemId, token);
            return ResponseEntity.ok(qty);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @GetMapping("/{shopId}/items/{itemId}/available")
    public ResponseEntity<?> checkSupply(
            @PathVariable int shopId,
            @PathVariable int itemId,
            @RequestParam String token) {
        try {
            boolean available = shopService.checkSupplyAvailability(shopId, itemId, token);
            return ResponseEntity.ok(available);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @PatchMapping("/{shopId}/items/{itemId}/supply/add")
    public ResponseEntity<?> addSupply(
            @PathVariable int shopId,
            @PathVariable int itemId,
            @RequestParam int supply,
            @RequestParam String token) {
        try {
            shopService.addSupply(shopId, itemId, supply, token);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @PostMapping("/{shopId}/items/{itemId}/supply/remove")
    public ResponseEntity<?> removeSupply(
            @PathVariable int shopId,
            @PathVariable int itemId,
            @RequestParam int supply,
            @RequestParam String token) {
        try {
            shopService.removeSupply(shopId, itemId, supply, token);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @PostMapping("/policy/check")
    public ResponseEntity<?> checkPolicy(
            @RequestBody HashMap<Integer, HashMap<Integer, Integer>> cart,
            @RequestParam String token) {
        try {
            boolean result = shopService.checkPolicy(cart, token);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @PostMapping("/{shopId}/purchase/{purchaseId}/ship")
    public ResponseEntity<?> shipPurchase(
            @PathVariable int shopId,
            @PathVariable int purchaseId,
            @RequestParam String country,
            @RequestParam String city,
            @RequestParam String street,
            @RequestParam String postalCode,
            @RequestParam String token) {
        try {
            shopService.shipPurchase(token, purchaseId, shopId, country, city, street, postalCode);
            return ResponseEntity.accepted().build(); // 202
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @GetMapping("/shops/ByWorkerId")
    public ResponseEntity<?> getShopsByWorkerId(
            @RequestParam int workerId,
            @RequestParam String token) {
        try {
            List<Shop> shops = shopService.getShopsByWorker(workerId, token);
            List<Item> items = shopService.getItems(token);
            List<ItemDTO> itemDTOs = items.stream()
                    .map(ItemDTO::fromDomain)
                    .toList();
            List<ShopDTO> shopDTOs = new ArrayList<>();
            for (Shop shop : shops) {
                shopDTOs.add(ShopDTO.fromDomain(shop, itemDTOs));
            }
            return ResponseEntity.ok(shopDTOs);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    // @PostMapping("/addDiscountPolicy")
    // public ResponseEntity<?> addDiscountPolicy(@RequestParam String token,
    //         @RequestParam int threshold,
    //         @RequestParam int itemId,
    //         @RequestParam ItemCategory category,
    //         @RequestParam double basketValue,
    //         @RequestParam Operator operator,
    //         @RequestParam int shopId) {
    //     try {
    //         shopService.addDiscountPolicy(token, threshold, itemId, category, basketValue, operator, shopId);
    //         return ResponseEntity.ok().build();
    //     } catch (Exception ex) {
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    //     }
    // }

    @PostMapping("/{shopId}/discount/policy")
    public ResponseEntity<?> setDiscountPolicy(
            @PathVariable int shopId,
            @RequestBody CompositePolicyDTO policyDto,
            @RequestParam String token) {
        try {
            //print the policy DTO for debugging
            System.out.println("Setting discount policy for shop " + shopId + ": " + policyDto);
            shopService.setDiscountPolicy(shopId, policyDto, token);
            System.out.println("Policy set successfully for shop " + shopId);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException|IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/{shopId}/discounts")
    public ResponseEntity<?> getDiscounts(
            @PathVariable int shopId,
            @RequestParam String token) {
        try {
            // fetch domain Discounts (service already validates token/permissions)
            List<Discount> discounts = shopService.getDiscounts(shopId, token);
            List<DiscountDTO> discountDTOs = discounts.stream()
                .map(DiscountDTO::fromDomain)
                .collect(Collectors.toList());

            return ResponseEntity.ok(discountDTOs);

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{shopId}/policies")
    public ResponseEntity<?> getPolicies(
            @PathVariable int shopId,
            @RequestParam String token) {
        try {
            List<Policy> domainPolicies = shopService.getPolicies(shopId, token);
            //print ("getPolicies");
            String s = ""; 
            for(Policy p : shopService.getPolicies(shopId, token)) {
                s += " " + p.toString();
            }

            System.out.println("policiesaflisdjgjghslf: " + s);

            // Map (Policy → item's itemId/itemCategory → frontend policiesDTO)
            List<PoliciesDTO> responseList = domainPolicies.stream()
                .map(p -> {
                    // If you need to know the itemId or category, fetch that from Discount.
                    // But since repository.getPolicies(...) only returned Policy, you may
                    // need a repository method to also pull (itemId,itemCategory) for each.
                    // Alternatively, you could embed that info in PolicyLeaf / PolicyComposite.
                    // For now, assuming each PolicyLeaf knows its metadata:
                    if (p instanceof PolicyLeaf leaf) {
                        return new PoliciesDTO(
                            leaf.getItemId(),         // assumes PolicyLeaf stores itemId internally
                            leaf.getCategory(),       // or category
                            CompositePolicyDTO.fromDomain(p)
                        );
                    }
                    if (p instanceof PolicyComposite comp) {
                        // Extract metadata from the composite’s leaves if needed…
                        // Or simply:
                        return new PoliciesDTO(
                            null,
                            null,
                            CompositePolicyDTO.fromDomain(p)
                        );
                    }
                    // fallback
                    return new PoliciesDTO(null, null, null);
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(responseList);

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // @PostMapping("/shops/{shopId}/cart/add")
    // public ResponseEntity<Void> addToCart(
    //     @PathVariable int shopId,
    //     @RequestParam int itemId,
    //     @RequestParam int quantity,
    //     @RequestParam String token
    // ) {
    //     try {
    //         shopService.addToCart(shopId, itemId, quantity, token);
    //         return ResponseEntity.status(HttpStatus.CREATED).build(); // 201
    //     } catch (ConstraintViolationException | IllegalArgumentException ex) {
    //         return ResponseEntity.badRequest().body(ex.getMessage()); // 400
    //     } catch (NoSuchElementException ex) {
    //         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404
    //     } catch (RuntimeException ex) {
    //         return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409
    //     } catch (Exception ex) {
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
    //     }
    // }
}
