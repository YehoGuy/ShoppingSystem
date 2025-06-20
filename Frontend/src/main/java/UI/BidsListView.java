// ───────────────────────────────────────────────────────────────────────────
// src/main/java/UI/BidsListView.java
// Customer‐facing screen: Show a table of all bids, and navigate
// to details when a row is clicked.
// ───────────────────────────────────────────────────────────────────────────
package UI;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import DTOs.BidRecieptDTO;

@Route(value = "bids", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class BidsListView extends VerticalLayout {

    private final String apiBase;
    private final String bidsBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Grid<BidRecieptDTO> bidGrid = new Grid<>(BidRecieptDTO.class, false);

    public BidsListView(@Value("${url.api}") String apiBase) {
        this.apiBase = apiBase;
        this.bidsBaseUrl = apiBase + "/purchases/bids";

        getUserId(); // Ensure userId is set in session
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Available Bids");
        add(header);

        // Configure the columns you want to show
        // Store Name
        bidGrid.addColumn(dto -> fetchShopName(dto.getStoreId()))
            .setHeader("Store Name")
            .setAutoWidth(true);

        // Item Name
        bidGrid.addColumn(dto -> fetchItemName(dto))
            .setHeader("Item Name")
            .setAutoWidth(true);

        // Owner Bid Name (the user who placed the highest/current bid)
        bidGrid.addColumn(dto -> fetchUserName(dto.getUserId()))
                .setHeader("Owner Bid Name")
                .setAutoWidth(true);
        bidGrid.addColumn(dto -> dto.getPrice())
                .setHeader("Initial Price")
                .setAutoWidth(true);
        bidGrid.addColumn(dto -> dto.getHighestBid())
                .setHeader("Highest Bid")
                .setAutoWidth(true);
        bidGrid.addColumn(dto -> dto.isCompleted() ? "Yes" : "No")
                .setHeader("Completed");

        add(bidGrid);
        // ————— Place New Bid button —————
        bidGrid.addComponentColumn(dto -> {
            Button placeBid = new Button("Place New Bid");
            // only allow non‐owners to bid, and only if the auction is still open
            boolean isOwner    = getUserId() != null && getUserId().equals(dto.getStoreId());
            boolean isComplete = dto.isCompleted();
            placeBid.setEnabled(!isOwner && !isComplete);
            placeBid.addClickListener(e ->
            UI.getCurrent().navigate(
                "bid/" + dto.getPurchaseId()
                )
            );
            return placeBid;
        })
        .setHeader("Place New Bid")
        .setAutoWidth(true);

        // ————— Finalize Bid button —————
        bidGrid.addComponentColumn(dto -> {
            Button btn = new Button("Finalize Bid");
            Integer me = getUserId();
            boolean isComplete = dto.isCompleted();

            // fetch shop owner
            boolean amOwner = false;
            try {
                String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
                String ownerUrl = apiBase
                                + "/users/shops/"
                                + dto.getStoreId()
                                + "/owner?token=" + token;
                ResponseEntity<Integer> ownerResp =
                    restTemplate.exchange(ownerUrl, HttpMethod.GET, null, Integer.class);
                if (ownerResp.getStatusCode().is2xxSuccessful()) {
                    Integer shopOwnerId = ownerResp.getBody();
                    amOwner = Objects.equals(me, shopOwnerId);
                }
            } catch (Exception ex) {
                // ignore or log
            }
            if (me != null && !(amOwner)) {
                return new Span();
            }
            btn.setEnabled(amOwner && !isComplete);
            btn.addClickListener(e -> finalizeBid(dto.getPurchaseId()));
            return btn;
        })
        .setHeader("Finalize Bid")
        .setAutoWidth(true);

        // ————— Accept Bid button —————
        bidGrid.addComponentColumn(dto -> {
            Integer me = getUserId();
            Integer originalBidder = dto.getUserId();          // the buyer who made this bid
            Double initialPrice   = dto.getPrice();
            Double ownerCounter   = Double.valueOf(dto.getHighestBid());      // null or same as initialPrice if no counter yet

            // 1) only show to the original bidder
            boolean isBidder     = Objects.equals(me, originalBidder);
            // 2) only show once ownerCounter > initialPrice
            boolean hasCounter   = ownerCounter != null && ownerCounter > initialPrice;
            // 3) only when the bid isn’t already completed
            boolean notFinished  = !dto.isCompleted();

            if (!(isBidder && hasCounter && notFinished)) {
                return new Span();   // empty placeholder
            }

            Button accept = new Button("Accept Bid");
            accept.addClickListener(evt -> {
                String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
                String url = bidsBaseUrl
                            + "/" + dto.getPurchaseId()
                            + "/accept?authToken=" + token;
                try {
                restTemplate.postForEntity(url, null, Void.class);
                Notification.show("You accepted the counter-offer!", 3000, Position.MIDDLE);
                fetchAllBids();
                } catch (HttpStatusCodeException ex) {
                Notification.show("Error: " + ex.getResponseBodyAsString(), 5000, Position.MIDDLE);
                } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Position.MIDDLE);
                }
            });
            return accept;
        })
        .setHeader("Accept Bid")
        .setAutoWidth(true);
        // Add a listener so that when a row is clicked, we navigate to
        // /bid/{purchaseId}
        bidGrid.asSingleSelect().addValueChangeListener(event -> {
            BidRecieptDTO selected = event.getValue();
            if (selected != null) {
                // This causes Vaadin to go to /bid/<selectedPurchaseId>
                getUI().ifPresent(ui -> ui.navigate("bid/" + selected.getPurchaseId()));
            }
        });

        fetchAllBids();
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    private void fetchAllBids() {
        try {
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            if (authToken == null || authToken.isBlank()) {
                add(new Text("You must log in to view bids."));
                return;
            }

            // Call GET /api/purchases/bids?authToken=<token>
            String urlWithToken = bidsBaseUrl + "?authToken=" + authToken;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<BidRecieptDTO>> response = restTemplate.exchange(
                    urlWithToken,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                bidGrid.setItems(response.getBody());
            } else {
                add(new Text("Failed to load bids"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            add(new Text("Error fetching bids"));
        }
    }

    private void finalizeBid(int purchaseId) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String url = bidsBaseUrl + "/" + purchaseId + "/finalize?authToken=" + token;

        try {
            // mirror your BidDetailView logic:
            ResponseEntity<Integer> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()),
                Integer.class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                Notification.show("Bid finalized!", 2000, Position.MIDDLE);
                fetchAllBids();
            } else {
                Notification.show("Could not finalize: " + resp.getStatusCode(),
                                  3000, Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("Error finalizing bid: " + ex.getMessage(),
                              4000, Position.MIDDLE);
        }
    }

    private String fetchShopName(int shopId) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String url = bidsBaseUrl.replace("/purchases/bids", "/shops/" + shopId) 
                + "?token=" + token;
        try {
            ResponseEntity<JsonNode> r = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                JsonNode.class
            );
            if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null) {
                return r.getBody().path("name").asText("");
            }
        } catch (Exception e) { /* log if you like */ }
        return "";
    }

    private String fetchUserName(int userId) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) return "";
        String url = apiBase + "/users/" + userId + "?token=" + token;
        try {
            ResponseEntity<JsonNode> r = restTemplate.exchange(
                url, 
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                JsonNode.class
            );
            if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null) {
                return r.getBody().path("username").asText("");
            }
        } catch (Exception e) { /* ignore or log */ }
        return "";
    }

    private String fetchItemName(BidRecieptDTO dto) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String url = bidsBaseUrl.replace("/purchases/bids", 
                    "/shops/" + dto.getStoreId() + "/items")
                + "?token=" + token;
        try {
            ResponseEntity<JsonNode> r = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                JsonNode.class
            );
            if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null) {
                for (JsonNode item : r.getBody()) {
                    int id = item.path("id").asInt(-1);
                    if (dto.getItems().containsKey(id)) {
                        return item.path("name").asText("");
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }
        return "";
    }

}