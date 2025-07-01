package UI;

import DTOs.BidRecieptDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Route(value = "bids", layout = AppLayoutBasic.class)
@AnonymousAllowed
@JsModule("./notification-client.js")
public class BidsListView extends BaseView {

    private final RestTemplate rest = new RestTemplate();
    private final Grid<BidRecieptDTO> bidGrid = new Grid<>(BidRecieptDTO.class, false);
    private final String apiBase;
    private final String bidsEndpoint;

    public BidsListView(@Value("${url.api}") String apiBase) {
        // Animated header: icon + title + arrow
        super("Bids", "Manage your offers", "💸", "➡️");
        this.apiBase      = apiBase;
        this.bidsEndpoint = apiBase + "/purchases/bids";

        // Fill the browser
        setSizeFull();

        // Wrap grid in a styled card
        H2 title = new H2("Available Bids");
        title.getStyle().set("margin-bottom", "1rem");

        bidGrid.setWidthFull();
        configureGrid();

        VerticalLayout card = new VerticalLayout(title, bidGrid);
        card.addClassName("view-card");
        card.setSizeFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
            .set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)");

        // Flex‐grow inside the card
        card.expand(bidGrid);

        // Add & flex‐grow the card in this view
        add(card);
        expand(card);

        // Load data
        // fetchAllBids();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        fetchAllBids();
    }

    private void configureGrid() {
        bidGrid.addColumn(dto -> fetchShopName(dto.getStoreId()))
               .setHeader("Store Name")
               .setAutoWidth(true);

        bidGrid.addColumn(dto -> fetchItemName(dto.getStoreId(), dto))
               .setHeader("Item Name")
               .setAutoWidth(true);

        bidGrid.addColumn(dto -> fetchUserName(dto.getUserId()))
               .setHeader("Owner Bid Name")
               .setAutoWidth(true);

        bidGrid.addColumn(BidRecieptDTO::getPrice)
               .setHeader("Initial Price")
               .setAutoWidth(true);

        bidGrid.addColumn(BidRecieptDTO::getHighestBid)
               .setHeader("Highest Bid")
               .setAutoWidth(true);

        bidGrid.addColumn(dto -> dto.isCompleted() ? "Yes" : "No")
               .setHeader("Completed")
               .setAutoWidth(true);

        // Place New Bid button
        bidGrid.addComponentColumn(dto -> {
            Button place = new Button("Place New Bid");
            if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
                place.setVisible(false);
            }
            boolean isOwner    = Objects.equals(getUserId(), dto.getStoreId());
            boolean isComplete = dto.isCompleted();
            place.setEnabled(!isOwner && !isComplete);
            place.addClickListener(e ->
                UI.getCurrent().navigate("bid/" + dto.getPurchaseId())
            );
            return place;
        }).setHeader("Place New Bid")
          .setAutoWidth(true);

        // Finalize Bid button
        bidGrid.addComponentColumn(dto -> {
            Button fin = new Button("Finalize Bid");
            Integer me = getUserId();
            boolean isComplete = dto.isCompleted();

            // Only shop-owner can finalize
            boolean amOwner = false;
            try {
                String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
                String url = apiBase + "/users/shops/" + dto.getStoreId() + "/owner?token=" + token;
                ResponseEntity<Integer> resp = rest.exchange(url, HttpMethod.GET, null, Integer.class);
                amOwner = resp.getStatusCode().is2xxSuccessful()
                          && Objects.equals(me, resp.getBody());
            } catch (Exception ignored) {}

            fin.setEnabled(amOwner && !isComplete);
            if (!amOwner) {
                return new Span(); // hide for others
            }
            fin.addClickListener(e -> finalizeBid(dto.getPurchaseId()));
            return fin;
        }).setHeader("Finalize Bid")
          .setAutoWidth(true);

        // Accept Bid button
        bidGrid.addComponentColumn(dto -> {
            Integer me = getUserId();
            Double initial = dto.getPrice();
            Double counter = Double.valueOf(dto.getHighestBid());
            boolean isBidder   = Objects.equals(me, dto.getUserId());
            boolean hasCounter = counter > initial;
            boolean notDone    = !dto.isCompleted();
            if (!(isBidder && hasCounter && notDone)) {
                return new Span();
            }
            Button accept = new Button("Accept Bid");
            accept.addClickListener(e -> {
                String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
                String url = bidsEndpoint + "/" + dto.getPurchaseId() + "/accept?authToken=" + token;
                try {
                    rest.postForEntity(url, null, Void.class);
                    Notification.show("You accepted the counter-offer!", 3000, Position.MIDDLE);
                    fetchAllBids();
                } catch (HttpStatusCodeException ex) {
                    Notification.show("Error: " + ex.getResponseBodyAsString(), 5000, Position.MIDDLE);
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage(), 5000, Position.MIDDLE);
                }
            });
            return accept;
        }).setHeader("Accept Bid")
          .setAutoWidth(true);

        // Row‐click navigation
        bidGrid.asSingleSelect().addValueChangeListener(evt -> {
            BidRecieptDTO sel = evt.getValue();
            if (sel != null) {
                UI.getCurrent().navigate("bid/" + sel.getPurchaseId());
            }
        });
    }

    private void fetchAllBids() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            add(new Text("You must log in to view bids."));
            return;
        }
        String url = bidsEndpoint + "?authToken=" + token;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<List<BidRecieptDTO>> resp = rest.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {}
        );
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            List<BidRecieptDTO> all = resp.getBody();
            List<BidRecieptDTO> filtered = all.stream()
                .filter(dto -> {
                    String name = fetchItemName(dto.getStoreId(), dto);
                    return name != null && !name.isEmpty();
                })
                .collect(Collectors.toList());
            bidGrid.setItems(filtered);
        } else {
            add(new Text("Failed to load bids"));
        }
    }

    private void finalizeBid(int purchaseId) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String url = bidsEndpoint + "/" + purchaseId + "/finalize?authToken=" + token;
        try {
            ResponseEntity<Integer> resp = rest.exchange(
                url, HttpMethod.POST, new HttpEntity<>(new HttpHeaders()), Integer.class
            );
            if (resp.getStatusCode().is2xxSuccessful()) {
                Notification.show("Bid finalized!", 2000, Position.MIDDLE);
                fetchAllBids();
            } else {
                Notification.show("Could not finalize: " + resp.getStatusCode(), 3000, Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("Error finalizing bid: " + ex.getMessage(), 4000, Position.MIDDLE);
        }
    }

    private Integer getUserId() {
        Object uid = VaadinSession.getCurrent().getAttribute("userId");
        if (uid != null) {
            return Integer.parseInt(uid.toString());
        }
        UI.getCurrent().navigate(""); 
        return null;
    }

    private String fetchShopName(int shopId) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String url = apiBase + "/shops/" + shopId + "?token=" + token;
        try {
            ResponseEntity<JsonNode> r = rest.exchange(
                url, HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class
            );
            return r.getBody() != null ? r.getBody().path("name").asText("") : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String fetchUserName(int userId) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) return "";
        String url = apiBase + "/users/" + userId + "?token=" + token;
        try {
            ResponseEntity<JsonNode> r = rest.exchange(
                url, HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class
            );
            return r.getBody() != null ? r.getBody().path("username").asText("") : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String fetchItemName(int shopId, BidRecieptDTO bid) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String url   = apiBase + "/shops/" + shopId + "/items?token=" + token;
        try {
            ResponseEntity<JsonNode> resp = rest.exchange(
                url, HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class
            );
            JsonNode body = resp.getBody();

            if (body != null && body.isArray() && body.size() > 0) {
                // 1) If our DTO map is empty, just grab the first element's name:
                if (bid.getItems().isEmpty()) {
                    JsonNode first = body.get(0);
                    int    fid   = first.path("id").asInt(-1);
                    String fname = first.path("name").asText("(no-name)");
                    return fname;
                }

                // 2) Otherwise do your normal matching:
                for (JsonNode item : body) {
                    int    id   = item.path("id").asInt(-1);
                    String name = item.path("name").asText("(no-name)");
                    if (bid.getItems().containsKey(id)) {
                        return name;
                    }
                }
            } else {
            }
        } catch (Exception e) {
            /*ignore */
        }
        return "";
    }
}
