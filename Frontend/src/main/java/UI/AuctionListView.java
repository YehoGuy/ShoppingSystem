package UI;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import DTOs.BidRecieptDTO;

/**
 * List all auctions with current status.
 */
@Route(value = "auctions", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class AuctionListView extends VerticalLayout {
    
    private final String apiBase;

    private final String auctionsBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Grid<BidRecieptDTO> auctionGrid = new Grid<>(BidRecieptDTO.class, false);

    public AuctionListView(@Value("${url.api}") String apiBase) {
        this.apiBase = apiBase;
        this.auctionsBaseUrl = apiBase + "/purchases/auctions";

        getUserId(); // Ensure userId is set in session
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Available Auctions");
        add(header);

        // Configure the columns you want to show
        auctionGrid.addColumn(dto -> getShopName(dto.getStoreId()))
                   .setHeader("Store Name")
                   .setAutoWidth(true);

        auctionGrid.addColumn(dto -> getItemName(dto.getStoreId(), dto))
                   .setHeader("Item Name")
                   .setAutoWidth(true);

        auctionGrid.addColumn(dto -> getUserName(dto.getUserId()))
                   .setHeader("Owner Auction Name")
                   .setAutoWidth(true);

        auctionGrid.addColumn(dto -> dto.getPrice())
                .setHeader("Initial Price")
                .setAutoWidth(true);
        auctionGrid.addColumn(dto -> dto.getHighestBid())
                .setHeader("Highest Bid")
                .setAutoWidth(true);
        auctionGrid.addColumn(dto -> dto.isCompleted() ? "Yes" : "No")
                .setHeader("Completed");

        add(auctionGrid);

        // add a button to add offer to auction
        auctionGrid.addColumn(new ComponentRenderer<>(dto -> {
        Button addOffer = new Button("Add Offer");
        addOffer.addClickListener(evt -> {
            Integer me = getUserId();
            if (me != null && me.equals(dto.getUserId())) {
                Notification.show(
                "You cannot place a bid on your own auction",
                3000,
                Position.MIDDLE
                );
            } else {
                UI.getCurrent().navigate("auction/" + dto.getPurchaseId());
            }
        });
        return addOffer;
        }))
        .setHeader("Auctions")
        .setAutoWidth(true);
        
        // ─── Time left column ───────────────────────────────────────────────
        auctionGrid.addColumn(new ComponentRenderer<>(dto -> {
            Span timer = new Span();
            Runnable update = () -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime end = dto.getEndTime();
                if (end == null) {
                    timer.setText("—");           // no data
                } else if (end.isBefore(now)) {
                    timer.setText("Ended");
                } else {
                    Duration d = Duration.between(now, end);
                    timer.setText(String.format(
                        "%02d:%02d:%02d",
                        d.toHours(),
                        d.toMinutesPart(),
                        d.toSecondsPart()));
                }
            };
            update.run();
            UI ui = UI.getCurrent();
            ui.setPollInterval(1000);
            ui.addPollListener(e -> update.run());
            return timer;
        }))
        .setHeader("Time Left")
        .setAutoWidth(true);

        fetchAllAuctions();
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    private void fetchAllAuctions() {
        try {
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            if (authToken == null || authToken.isBlank()) {
                add(new Text("You must log in to view auctions."));
                return;
            }

            // Call GET /api/purchases/auctions?authToken=<token>
            String urlWithToken = auctionsBaseUrl + "?authToken=" + authToken;
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
                auctionGrid.setItems(response.getBody());
            } else {
                add(new Text("Failed to load auctions"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            add(new Text("Error fetching auctions"));
        }
    }

    private String getShopName(int shopId) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) return "";
        String url = apiBase + "/shops/" + shopId + "?token=" + token;
        try {
            ResponseEntity<JsonNode> r = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                JsonNode.class
            );
            if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null) {
                return r.getBody().path("name").asText("");
            }
        } catch (Exception e) { /* ignore or log */ }
        return "";
    }

    private String getUserName(int userId) {
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

    private String getItemName(int shopId, BidRecieptDTO bid) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) return "";
        String url = apiBase + "/shops/" + shopId + "/items?token=" + token;
        try {
            ResponseEntity<JsonNode> r = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                JsonNode.class
            );
            if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null) {
                for (JsonNode item : r.getBody()) {
                    int id = item.path("id").asInt(-1);
                    if (bid.getItems().containsKey(id)) {
                        return item.path("name").asText("");
                    }
                }
            }
        } catch (Exception e) { /* ignore or log */ }
        return "";
    }
}