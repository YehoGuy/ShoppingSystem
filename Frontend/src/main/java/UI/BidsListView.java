// ───────────────────────────────────────────────────────────────────────────
// src/main/java/UI/BidsListView.java
// Customer‐facing screen: Show a table of all bids, and navigate
// to details when a row is clicked.
// ───────────────────────────────────────────────────────────────────────────
package UI;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

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

    private final String bidsBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Grid<BidRecieptDTO> bidGrid = new Grid<>(BidRecieptDTO.class, false);

    public BidsListView(@Value("${url.api}") String apiBase) {
        this.bidsBaseUrl = apiBase + "/purchases/bids";

        getUserId(); // Ensure userId is set in session
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Available Bids");
        add(header);

        // Configure the columns you want to show
        bidGrid.addColumn(BidRecieptDTO::getPurchaseId)
                .setHeader("Bid ID")
                .setAutoWidth(true);
        bidGrid.addColumn(BidRecieptDTO::getStoreId)
                .setHeader("Store ID")
                .setAutoWidth(true);
        bidGrid.addColumn(dto -> dto.getPrice())
                .setHeader("Current Price")
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
        bidGrid.addColumn(new ComponentRenderer<>(dto -> {
            Integer currentUserId = (Integer) VaadinSession.getCurrent()
                                            .getAttribute("userId");
            // Only shop owner sees “Finalize”
            if (dto.getStoreId() != currentUserId || dto.isCompleted()) {
                return new Span();  // empty placeholder
            }

            Button finalize = new Button("Finalize", evt -> {
                String token = (String) VaadinSession.getCurrent()
                                    .getAttribute("authToken");
                String url = bidsBaseUrl 
                        + "/" + dto.getPurchaseId() 
                        + "/finalize?token=" + token;
                try {
                    ResponseEntity<Integer> resp = restTemplate.postForEntity(url, null, Integer.class);
                    if (resp.getStatusCode().is2xxSuccessful()) {
                        Notification.show("Auction closed! Winner: user " + resp.getBody(),
                                        2500, Position.MIDDLE);
                        fetchAllBids();
                    } else {
                        Notification.show("Could not finalize: " + resp.getStatusCode(),
                                        3000, Position.MIDDLE);
                    }
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage(), 4000, Position.MIDDLE);
                }
            });
            return finalize;
        }))
        .setHeader("Finalize")
        .setAutoWidth(true);

        // ————— Accept Bid button —————
        bidGrid.addComponentColumn(dto -> {
            Integer currentUserId = getUserId();
            // only show to the winner, once the auction is completed
            if (!dto.isCompleted() || !currentUserId.equals(dto.getHighestBidderId())) {
                return new Span();
            }

            Button accept = new Button("Accept Bid");
            accept.addClickListener(evt -> {
                String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
                String url = bidsBaseUrl 
                        + "/" + dto.getPurchaseId() 
                        + "/accept?authToken=" + token;
                try {
                    restTemplate.postForEntity(url, null, Void.class);
                    Notification.show("You accepted the bid! The shop owner has been notified.",
                                    3000, Position.MIDDLE);
                    fetchAllBids(); // refresh the table
                } catch (Exception ex) {
                    Notification.show("Error accepting bid: " + ex.getMessage(),
                                    5000, Position.MIDDLE);
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
}