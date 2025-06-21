package com.example.app.PresentationLayer.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.example.app.ApplicationLayer.Purchase.PurchaseService;
import com.example.app.PresentationLayer.DTO.Purchase.PaymentDetailsDTO;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.PresentationLayer.DTO.Purchase.BidRecieptDTO;
import com.example.app.PresentationLayer.DTO.Purchase.RecieptDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;

/**
 * Base path: /api/purchases (all calls are JSON in / JSON out)
 *
 * 1. POST /checkout
 * Params : authToken, country, city, street, houseNumber, [zipCode]
 * Success : 201 → [ 123, 124 ] (array of purchase IDs)
 *
 * 2. POST /bids
 * Params : authToken, storeId, initialPrice
 * Body : { "5": 2, "9": 1 } // itemId → quantity
 * Success : 201 → 42 (the new bid ID)
 *
 * 3. POST /bids/{bidId}/offers
 * Params : authToken, bidAmount
 * Success : 202 (empty body)
 *
 * 4. POST /bids/{bidId}/finalize
 * Params : authToken
 * Success : 200 → 17 (winning bidder’s user‑id)
 * 
 * 5. GET /api/purchases/users/{userId}
 * Params : authToken
 * Success: 200 → [ RecieptDTO, … ]
 * 
 * 6. GET /bids
 * Params : authToken
 * Success: 200 → [ BidRecieptDTO, … ]
 * 
 * 7. GET /bids/{bidId}
 * Params : authToken
 * Success: 200 → BidRecieptDTO
 *
 * Error mapping (all endpoints)
 * 400 – Bad data / validation failure
 * 404 – Entity not found (store, bid, cart…)
 * 409 – Business rule conflict (e.g., bid closed, out‑of‑stock)
 * 500 – Unhandled server error
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
            @RequestParam String zipCode,
            @RequestBody PaymentDetailsDTO paymentDetails) {

        try {
            // compose Address inline
            com.example.app.DomainLayer.Purchase.Address shipping = new com.example.app.DomainLayer.Purchase.Address()
                    .withCity(city)
                    .withCountry(country)
                    .withStreet(street)
                    .withHouseNumber(houseNumber)
                    .withZipCode(zipCode);

            List<Integer> ids = purchaseService.checkoutCart(authToken, shipping, paymentDetails.getCurrency(),
                    paymentDetails.getCardNumber(), paymentDetails.getExpirationDateMonth(),
                    paymentDetails.getExpirationDateYear(), paymentDetails.getCardHolderName(),
                    paymentDetails.getCvv(), paymentDetails.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(ids); // 201 Created

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            // bad parameters (e.g., empty city or invalid token format)
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400

        } catch (NoSuchElementException ex) {
            // guest/member or cart not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404

        } catch (RuntimeException ex) {
            // business conflict (e.g., inventory unavailable)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409

        } catch (Exception ex) {
            // anything unexpected
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }

    
    @PostMapping("/partial-checkout")
    public ResponseEntity<?> partialheckout(
            @RequestParam String authToken,
            @RequestParam String country,
            @RequestParam String city,
            @RequestParam String street,
            @RequestParam String houseNumber,
            @RequestParam String zipCode,
            @RequestParam int shopId,
            @RequestBody PaymentDetailsDTO paymentDetails) {

        try {
            // compose Address inline
            com.example.app.DomainLayer.Purchase.Address shipping = new com.example.app.DomainLayer.Purchase.Address()
                    .withCity(city)
                    .withCountry(country)
                    .withStreet(street)
                    .withHouseNumber(houseNumber)
                    .withZipCode(zipCode);

            List<Integer> ids = purchaseService.partialCheckoutCart(authToken, shipping, paymentDetails.getCurrency(),
                    paymentDetails.getCardNumber(), paymentDetails.getExpirationDateMonth(),
                    paymentDetails.getExpirationDateYear(), paymentDetails.getCardHolderName(),
                    paymentDetails.getCvv(), paymentDetails.getId(), shopId);
            return ResponseEntity.status(HttpStatus.CREATED).body(ids); // 201 Created

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            // bad parameters (e.g., empty city or invalid token format)
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400

        } catch (NoSuchElementException ex) {
            // guest/member or cart not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404

        } catch (RuntimeException ex) {
            // business conflict (e.g., inventory unavailable)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409

        } catch (Exception ex) {
            // anything unexpected
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }


    
    /* ─────────────────────────── BIDS ────────────────────────── */

    @PostMapping("/bids")
    public ResponseEntity<?> createBid(
            @RequestParam String authToken,
            @RequestParam int storeId,
            @RequestBody Map<Integer, Integer> items, // JSON: { "5":2, "9":1 }
            @RequestParam int initialPrice) {

        try {
            int bidId = purchaseService.createBid(authToken, storeId, items, initialPrice);
            return ResponseEntity.status(HttpStatus.CREATED).body(bidId); // 201, plain int

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            // malformed parameters (e.g., negative price, empty items)
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400

        } catch (NoSuchElementException ex) {
            // store not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404

        } catch (RuntimeException ex) {
            // business conflict, e.g., “store closed for bids”
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409

        } catch (Exception ex) {
            // unexpected failure
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }

    @PostMapping("/bids/{bidId}/offers")
    public ResponseEntity<String> postBidOffer(
            @PathVariable @Min(1) int bidId,
            @RequestParam String authToken,
            @RequestParam @Min(1) int bidPrice) {

        try {
            purchaseService.postBidding(authToken, bidId, bidPrice);
            // Success: 202 Accepted, no body needed
            return ResponseEntity.accepted().build();

        } catch (IllegalArgumentException | ConstraintViolationException ex) {
            // Bad request: include the exception message in the body
            return ResponseEntity
                    .badRequest()
                    .body(ex.getMessage());

        } catch (RuntimeException ex) {
            // Conflict: include the domain‐exception message in the body
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());

        } catch (Exception ex) {
            // Internal server error: include a generic message
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error");
        }
    }


    @PostMapping("/bids/{bidId}/finalize")
    public ResponseEntity<?> finalizeBid(
            @PathVariable @Min(1) int bidId,
            @RequestParam String authToken) {

        try {
            // Parse the payment details JSON
            int winnerId = purchaseService.finalizeBid(authToken, bidId, false);
            // success → 200 OK, return the winner's user‑id
            return ResponseEntity.ok(winnerId);
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            // bad parameters (e.g., negative id, malformed token) → 400
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (NoSuchElementException ex) {
            // bid doesn't exist → 404
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

    @GetMapping("/bids")
    public ResponseEntity<?> getAllBids(
            @RequestParam String authToken) {

        try {
            List<BidReciept> bids = purchaseService.getAllBids(authToken, true);
            List<BidRecieptDTO> bidDTOs = bids.stream()
                    .map(BidRecieptDTO::fromDomain) // convert to DTO
                    .toList();
            return ResponseEntity.ok(bidDTOs); // 200 OK, returns list of bids

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }

    @GetMapping("/bids/finished")
    public ResponseEntity<List<BidRecieptDTO>> getFinishedBidsSection(
            @RequestParam String authToken) {
        try {
            List<BidReciept> finishedBids = purchaseService.getFinishedBidsList(authToken);
            // map domain‐model receipts → DTOs
            List<BidRecieptDTO> dtos = finishedBids.stream()
                .map(BidRecieptDTO::fromDomain)  
                .toList();
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException ex) {
            // token invalid
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/shops/{shopId}/bids")
    public ResponseEntity<?> getStoreBids(
            @PathVariable @Min(1) int shopId,
            @RequestParam String authToken) {

        try {
            List<BidReciept> bids = purchaseService.getShopBids(authToken, shopId);
            List<BidRecieptDTO> bidDTOs = bids.stream()
                    .map(BidRecieptDTO::fromDomain) // convert to DTO
                    .toList();
            return ResponseEntity.ok(bidDTOs); // 200 OK, returns list of bids

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }


    @GetMapping("/bids/{bidId}")
    public ResponseEntity<?> getBid(
            @PathVariable @Min(1) int bidId,
            @RequestParam String authToken) {

        try {
            BidReciept rec = purchaseService.getBid(authToken, bidId);
            return ResponseEntity.ok(BidRecieptDTO.fromDomain(rec)); // 200

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }

    @PostMapping("/bids/{bidId}/accept")
    public ResponseEntity<String> acceptBid(
            @PathVariable @Min(1) int bidId,
            @RequestParam String authToken) {
        try {
            purchaseService.acceptBid(authToken, bidId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
    }

    // ────────────────────────── GET PURCHASES ────────────────────────── */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserPurchases(
            @PathVariable @Min(1) int userId,
            @RequestParam String authToken) {

        try {
            List<RecieptDTO> receipts = purchaseService
                    .getUserPurchases(authToken, userId) // domain list
                    .stream()
                    .map(RecieptDTO::fromDomain) // → DTO
                    .toList();

            return ResponseEntity.ok(receipts); // 200

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
        }
    }

    @GetMapping("/{purchaseId}")
    public ResponseEntity<?> getReciept(
            @PathVariable @Min(1) int purchaseId,
            @RequestParam String authToken) {

        try {
            RecieptDTO receipt = purchaseService.getReciept(purchaseId)
                    .stream().map(RecieptDTO::fromDomain).toList().get(0);
            return ResponseEntity.ok(receipt); // 200

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage()); // 400

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); // 404

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage()); // 409

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error"); // 500
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

    /* ─────────────────────────── AUCTIONS ────────────────────────── */

    @PostMapping("/auctions")
    public ResponseEntity<Integer> startAuction(
            @RequestParam String authToken,
            @RequestParam @Min(1) int storeId,
            @RequestBody Map<Integer, Integer> items,
            @RequestParam @Min(0) int initialPrice,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime auctionEndTime
    ) {
        try {
            int auctionId = purchaseService.startAuction(
                authToken, storeId, items, initialPrice, auctionEndTime
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(auctionId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(-1);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(-1);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(-1);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(-1);
        }
    }

    @PostMapping("/auctions/{auctionId}/offers")
    public ResponseEntity<Void> placeAuctionBid(
            @PathVariable @Min(1) int auctionId,
            @RequestParam String authToken,
            @RequestParam @Min(1) int bidAmount
    ) {
        try {
            purchaseService.postBiddingAuction(authToken, auctionId, bidAmount);
            return ResponseEntity.accepted().build();
        } catch (IllegalArgumentException | ConstraintViolationException ex) {
            //System.out.println("Invalid parameters (400): " + ex.getMessage());
            return ResponseEntity.badRequest().body(null); // 400
        } catch (NoSuchElementException ex) {
            //System.out.println("Invalid parameters (404): " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 404
        } catch (RuntimeException ex) {
            //System.out.println("Invalid parameters (409): " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null); // 409
        } catch (Exception ex) {
            //System.out.println("Invalid parameters (500): " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // 500
        }
    }

    @PostMapping("/auctions/{auctionId}/finalize")
    public ResponseEntity<?> finalizeAuctionInternal(
            @PathVariable("auctionId") @Min(1) int auctionId,
            @RequestParam String authToken) {

        try {
            // Parse the payment details JSON
            
            int winnerId = purchaseService.finalizeBid(authToken, auctionId, false);
            // success → 200 OK, return the winner's user‑id
            return ResponseEntity.ok(winnerId);
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            // bad parameters (e.g., negative id, malformed token) → 400
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (NoSuchElementException ex) {
            // bid doesn't exist → 404
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

    @GetMapping("/auctions")
    public ResponseEntity<List<BidRecieptDTO>> listAuctions(
            @RequestParam String authToken) {
        try {
            List<BidReciept> domain = purchaseService.getAllBids(authToken, false);
            List<BidRecieptDTO> dtos = domain.stream()
                .map(BidRecieptDTO::fromDomain)
                .toList();
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException | NoSuchElementException ex) {
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    @GetMapping("/auctions/{auctionId}")
    public ResponseEntity<BidRecieptDTO> getAuctionDetails(
            @PathVariable @Min(1) int auctionId,
            @RequestParam String authToken) {

        try {
            BidReciept rec = purchaseService.getBid(authToken, auctionId);
            return ResponseEntity.ok(BidRecieptDTO.fromDomain(rec)); // 200

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null); // 400

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 404

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null); // 409

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // 500
        }
    }

    

    

}
