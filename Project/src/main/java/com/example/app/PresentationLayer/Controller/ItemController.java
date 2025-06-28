package com.example.app.PresentationLayer.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.ApplicationLayer.Item.ItemService;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.PresentationLayer.DTO.Item.ItemDTO;
import com.example.app.PresentationLayer.DTO.Item.ItemReviewDTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Base path: /api/items (JSON in / JSON out)
 *
 * 1. POST /create
 * Params : shopId, name, description, category, token
 * Success: 201 → 123 (new item ID)
 *
 * 2. GET /{itemId}
 * Params : token
 * Success: 200 → ItemDTO as JSON
 *
 * 3. GET /all
 * Params : token
 * Success: 200 → [ItemDTO,...]
 *
 * 4. POST /{itemId}/reviews
 * Params : rating, reviewText, token
 * Success: 202 (empty)
 *
 * 5. GET /{itemId}/reviews
 * Params : token
 * Success: 200 → [ItemReviewDTO,...]
 *
 * 6. GET /{itemId}/rating
 * Params : token
 * Success: 200 → 4.5 (average rating)
 *
 * 7. POST /by-ids
 * Body : [1,2,3]
 * Params : token
 * Success: 200 → [ItemDTO,...]
 *
 * 8. GET /category
 * Params : category, token
 * Success: 200 → [1,5,7] (item IDs)
 *
 * Error mapping (all endpoints):
 * 400 – Bad data / validation failure
 * 404 – Entity not found
 * 409 – Business rule conflict (e.g., no permission)
 * 500 – Internal server error
 */
@RestController
@RequestMapping("/api/items")
@Validated
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /* ──────────────────── 1. CREATE ITEM ────────────────────── */
    @PostMapping("/create")
    public ResponseEntity<?> createItem(
            @RequestParam @Min(0) int shopId,
            @RequestParam @NotBlank String name,
            @RequestParam @NotBlank String description,
            @RequestParam ItemCategory category,
            @RequestParam @NotBlank String token) {
        try {
            Integer id = itemService.createItem(shopId, name, description, category, token);
            return ResponseEntity.status(HttpStatus.CREATED).body(id);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }

    /* ───────────────────── 2. GET ONE ITEM ─────────────────── */
    @GetMapping("/{itemId}")
    public ResponseEntity<?> getItem(
            @PathVariable @Min(0) int itemId,
            @RequestParam @NotBlank String token) {
        try {
            Item item = itemService.getItem(itemId, token);
            ItemDTO itemDTO = ItemDTO.fromDomain(item);
            return ResponseEntity.ok(itemDTO);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400

        } catch (OurRuntime ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }

    /* ───────────────────── 3. GET ALL ITEMS ─────────────────── */
    @GetMapping("/all")
    public ResponseEntity<?> getAllItems(
            @RequestParam @NotBlank String token) {
        try {
            List<Item> items = itemService.getAllItems(token);
            ItemDTO[] dtos = new ItemDTO[items.size()];
            for (int i = 0; i < items.size(); i++) {
                dtos[i] = ItemDTO.fromDomain(items.get(i));
            }
            return ResponseEntity.ok(dtos);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (OurRuntime ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    /* ──────────────────── 4. ADD ITEM REVIEW ────────────────── */
    @PostMapping("/{itemId}/reviews")
    public ResponseEntity<?> addReview(
            @PathVariable @Min(1) int itemId,
            @RequestParam @Min(1) @Max(5) int rating,
            @RequestParam String reviewText,
            @RequestParam String token) {
        try {
            itemService.addReviewToItem(itemId, rating, reviewText, token);
            return ResponseEntity.accepted().build(); // 202

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build(); // 400

        } catch (OurRuntime ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
        }
    }

    /* ──────────────────── 5. LIST ITEM REVIEWS ───────────────── */
    @GetMapping("/{itemId}/reviews")
    public ResponseEntity<?> getReviews(
            @PathVariable @Min(0) int itemId,
            @RequestParam @NotBlank String token) {
        try {
            List<ItemReviewDTO> reviews = itemService.getItemReviews(itemId, token).stream()
                    .map(ItemReviewDTO::fromDomain).toList();
            return ResponseEntity.ok(reviews);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400

        } catch (OurRuntime ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }

    /* ─────────────────── 6. GET ITEM RATING ─────────────────── */
    @GetMapping("/{itemId}/rating")
    public ResponseEntity<?> getRating(
            @PathVariable @Min(0) int itemId,
            @RequestParam @NotBlank String token) {
        try {
            double avg = itemService.getItemAverageRating(itemId, token);
            return ResponseEntity.ok(avg);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (OurRuntime ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }

    /* ────────────────── 7. GET ITEMS BY IDS ─────────────────── */
    @PostMapping("/by-ids")
    public ResponseEntity<?> getByIds(
            @RequestBody List<@Min(0) Integer> itemIds,
            @RequestParam @NotBlank String token) {
        try {
            List<Item> items = itemService.getItemsByIds(itemIds, token);
            List<ItemDTO> itemDTOs = items.stream().map(ItemDTO::fromDomain).toList();
            return ResponseEntity.ok(itemDTOs);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (OurRuntime ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }

    /* ──────────────────── 8. GET ITEMS BY CATEGORY ───────────── */
    @GetMapping("/category")
    public ResponseEntity<?> getByCategory(
            @RequestParam ItemCategory category,
            @RequestParam @NotBlank String token) {
        try {
            List<Integer> ids = itemService.getItemsByCategory(category, token);
            return ResponseEntity.ok(ids);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (OurRuntime ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }


    /** ─────────── 9. DELETE AN ITEM ─────────── */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable @Min(1) int itemId,
            @RequestParam("token") @NotBlank String token) {
        try {
            itemService.deleteItem(itemId, token);
            return ResponseEntity.noContent().build(); // 204
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();  // 400
        } catch (OurRuntime ex) {
            // e.g. “you don’t own that shop” or business rule
            return ResponseEntity.status(HttpStatus.CONFLICT).build();  // 409
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();  // 500
        }
    }
}
