package PresentationLayer.Controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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

import ApplicationLayer.Shop.ShopService;
import DomainLayer.Item.Item;
import DomainLayer.Item.ItemCategory;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;

/**
 *  Base path: /api/shops         (all calls are JSON in / JSON out)
 *
 * 1. POST   /create
 *    Params : token, name, shippingMethod
 *    Success: 201 → Shop as JSON
 *
 * 2. GET    /{shopId}
 *    Params : token
 *    Success: 200 → Shop as JSON
 *
 * 3. GET    /all
 *    Params : token
 *    Success: 200 → [ Shop, … ]
 *
 * 4. Other mutating / query endpoints map 1‑to‑1 with ShopService methods
 *
 *  Error mapping (all endpoints)
 *    400 – Bad data / validation failure
 *    404 – Entity not found
 *    409 – Business rule conflict
 *    500 – Unhandled server error
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
            // TODO: parse a real PurchasePolicy if needed
            var shop = shopService.createShop(name, null, null, token);
            return ResponseEntity.status(HttpStatus.CREATED).body(shop);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());        // 400
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Internal server error");            // 500
        }
    }

    /* ───────────────────────── 2. GET ONE SHOP ──────────────────────── */

    @GetMapping("/{shopId}")
    public ResponseEntity<?> getShop(
            @PathVariable @Min(1) int shopId,
            @RequestParam String token) {

        try {
            var shop = shopService.getShop(shopId, token);
            return ResponseEntity.ok(shop);

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());  // 409
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    /* ───────────────────────── 3. GET ALL SHOPS ─────────────────────── */

    @GetMapping("/all")
    public ResponseEntity<?> getAllShops(@RequestParam String token) {
        try {
            return ResponseEntity.ok(shopService.getAllShops(token));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    /* ───────────────────────── 4. DISCOUNTS & POLICY ────────────────── */

    @PostMapping("/{shopId}/policy")
    public ResponseEntity<?> updatePolicy(
            @PathVariable int shopId,
            @RequestParam String token) {
        try {
            // TODO: accept real policy params
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
            return ResponseEntity.accepted().build();  // 202
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
            @RequestParam int price,
            @RequestParam String token) {
        try {
            shopService.addItemToShop(shopId, name, description, quantity, price, token);
            return ResponseEntity.status(HttpStatus.CREATED).build();       // 201
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{shopId}/items/{itemId}/price")
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
            return ResponseEntity.ok(items);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/items")
    public ResponseEntity<?> listAllItems(@RequestParam String token) {
        try {
            return ResponseEntity.ok(shopService.getItems(token));
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
            return ResponseEntity.ok(items);
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
            return ResponseEntity.ok(items);
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

//adding from here
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

    @PostMapping("/{shopId}/purchase")
    public ResponseEntity<?> purchaseItems(
            @PathVariable int shopId,
            @RequestBody Map<Integer, Integer> purchaseLists,
            @RequestParam String token) {
        try {
            double total = shopService.purchaseItems(purchaseLists, shopId, token);
            return ResponseEntity.ok(total);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @PostMapping("/{shopId}/purchase/rollback")
    public ResponseEntity<?> rollbackPurchase(
            @PathVariable int shopId,
            @RequestBody Map<Integer, Integer> purchaseLists) {
        try {
            shopService.rollBackPurchase(purchaseLists, shopId);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @GetMapping("/{shopId}/items/{itemId}/reserve")
    public ResponseEntity<?> checkAndAcquire(
            @PathVariable int shopId,
            @PathVariable int itemId,
            @RequestParam int supply) {
        try {
            boolean success = shopService.checkSupplyAvailabilityAndAcquire(shopId, itemId, supply);
            return ResponseEntity.ok(success);
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


    @PatchMapping("/{shopId}/items/{itemId}/supply/remove")
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

}


