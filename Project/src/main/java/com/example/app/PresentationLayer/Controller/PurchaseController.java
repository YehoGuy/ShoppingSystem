package com.example.app.PresentationLayer.Controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.ApplicationLayer.Purchase.PurchaseService;
import com.example.app.PresentationLayer.DTO.Purchase.RecieptDTO;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;

/**
 *  Base path: /api/purchases         (all calls are JSON in / JSON out)
 *
 * 1. POST /checkout
 *    Params  : authToken, country, city, street, houseNumber, [zipCode]
 *    Success : 201  →  [ 123, 124 ]          (array of purchase IDs)
 *
 * 2. POST /bids
 *    Params  : authToken, storeId, initialPrice
 *    Body    : { "5": 2, "9": 1 }            // itemId → quantity
 *    Success : 201  →  42                    (the new bid ID)
 *
 * 3. POST /bids/{bidId}/offers
 *    Params  : authToken, bidAmount
 *    Success : 202  (empty body)
 *
 * 4. POST /bids/{bidId}/finalize
 *    Params  : authToken
 *    Success : 200  →  17                    (winning bidder’s user‑id)
 * 
 * 5. GET /api/purchases/users/{userId}
 *    Params : authToken
 *    Success: 200 → [ RecieptDTO, … ]
 *
 *  Error mapping (all endpoints)
 *    400 – Bad data / validation failure
 *    404 – Entity not found (store, bid, cart…)
 *    409 – Business rule conflict (e.g., bid closed, out‑of‑stock)
 *    500 – Unhandled server error
 */


@RestController
@RequestMapping("/api/purchases")
@Validated
public class PurchaseController {

    private final PurchaseService purchaseService;

    // Constructor injection — Spring will supply the bean
    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    /* ────────────────────────── CHECKOUT ───────────────────────── */

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @RequestParam String authToken,
            @RequestParam String country,
            @RequestParam String city,
            @RequestParam String street,
            @RequestParam String houseNumber,
            @RequestParam(required = false) String zipCode) {

        try {
            // compose Address inline
            com.example.app.DomainLayer.Purchase.Address shipping = new com.example.app.DomainLayer.Purchase.Address()
                    .withCity(city)
                    .withCountry(country)
                    .withStreet(street)
                    .withHouseNumber(houseNumber)
                    .withZipCode(zipCode);

            List<Integer> ids = purchaseService.checkoutCart(authToken, shipping);
            return ResponseEntity.status(HttpStatus.CREATED).body(ids);          // 201 Created

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            // bad parameters (e.g., empty city or invalid token format)
            return ResponseEntity.badRequest().body(ex.getMessage());            // 400

        } catch (NoSuchElementException ex) {
            // guest/member or cart not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404

        } catch (RuntimeException ex) {
            // business conflict (e.g., inventory unavailable)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());  // 409

        } catch (Exception ex) {
            // anything unexpected
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Internal server error");                 // 500
        }
    }

    /* ───────────────────────────  BIDS  ────────────────────────── */

    @PostMapping("/bids")
    public ResponseEntity<?> createBid(
            @RequestParam String authToken,
            @RequestParam int storeId,
            @RequestBody Map<Integer, Integer> items,     // JSON: { "5":2, "9":1 }
            @RequestParam int initialPrice) {

        try {
            int bidId = purchaseService.createBid(authToken, storeId, items, initialPrice);
            return ResponseEntity.status(HttpStatus.CREATED).body(bidId);   // 201, plain int

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            // malformed parameters (e.g., negative price, empty items)
            return ResponseEntity.badRequest().body(ex.getMessage());       // 400

        } catch (NoSuchElementException ex) {
            // store not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404

        } catch (RuntimeException ex) {
            // business conflict, e.g., “store closed for bids”
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());  // 409

        } catch (Exception ex) {
            // unexpected failure
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Internal server error");                          // 500
        }
    }

    @PostMapping("/bids/{bidId}/offers")
    public ResponseEntity<Void> postBidOffer(
            @PathVariable @Min(1) int bidId,
            @RequestParam String authToken,
            @RequestParam @Min(1) int bidAmount) {

        try {
            purchaseService.postBidding(authToken, bidId, bidAmount);
            // success: 202 Accepted, empty body
            return ResponseEntity.accepted().build();

        } catch (IllegalArgumentException | ConstraintViolationException ex) {
            // bad input from the client
            return ResponseEntity.badRequest().build();

        } catch (RuntimeException ex) {
            // domain/business conflict (e.g., bidding on closed auction)
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (Exception ex) {
            // any unexpected error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/bids/{bidId}/finalize")
    public ResponseEntity<?> finalizeBid(
            @PathVariable @Min(1) int bidId,
            @RequestParam String authToken) {

        try {
            int winnerId = purchaseService.finalizeBid(authToken, bidId);
            // success → 200 OK, return the winner’s user‑id
            return ResponseEntity.ok(winnerId);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            // bad parameters (e.g., negative id, malformed token) → 400
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (NoSuchElementException ex) {
            // bid doesn’t exist → 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());

        } catch (RuntimeException ex) {
            // business‑rule violations (e.g., bid already finalized) → 409
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        } catch (Exception ex) {
            // anything unexpected → 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Internal server error");
        }
    }

    // ────────────────────────── GET PURCHASES ────────────────────────── */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserPurchases(
            @PathVariable @Min(1) int userId,
            @RequestParam String authToken) {

        try {
            List<RecieptDTO> receipts = purchaseService
                    .getUserPurchases(authToken, userId)           // domain list
                    .stream()
                    .map(RecieptDTO::fromDomain)                   // → DTO
                    .toList();

            return ResponseEntity.ok(receipts);                    // 200

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());          // 400

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());  // 409

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Internal server error");                    // 500
        }
    }

    /** 6. GET all purchases made *in* a specific store */
    @GetMapping("/shops/{shopId}")
    public ResponseEntity<?> getStorePurchases(
            @PathVariable @Min(1) int shopId,
            @RequestParam String authToken) {
        try {
            List<RecieptDTO> receipts = purchaseService
                .getStorePurchases(authToken, shopId) // returns List<Reciept> in domain
                .stream()
                .map(RecieptDTO::fromDomain)          // map to DTO
                .toList();
            return ResponseEntity.ok(receipts);     // 200 + JSON array

        } catch (ConstraintViolationException|IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());            // 400
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());  // 409
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Internal server error");                 // 500
        }
    }

}
