package UI;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.router.*;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import DTOs.BidRecieptDTO;

@Route(value = "bid/:bidId", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class BidDetailView extends BaseView implements BeforeEnterObserver {

    private String bidIdStr;
    private int bidId;
    private BidRecieptDTO bid;

    private final RestTemplate rest = new RestTemplate();
    private final String apiBase;
    private final String BID_API_URL;

    private final TextField storeNameField    = new TextField("Store Name");
    private final TextField itemNameField     = new TextField("Item Name");
    private final TextField userNameField     = new TextField("Bid's Owner Name");
    private final TextField highestBidField   = new TextField("Highest Bid");
    private final TextField completedField    = new TextField("Completed");

    private final IntegerField newBid         = new IntegerField("Your Bid");
    private final Button placeBidButton       = new Button("Place Bid");

    public BidDetailView(@Value("${url.api}") String apiBase) {
        // super(title, subtitle, iconLeft, iconRight)
        super("Bid Details", "Inspect & place your offer", "💰", "➡️");

        this.apiBase     = apiBase;
        this.BID_API_URL = apiBase + "/purchases/bids";

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    private Integer getUserId() {
        Object uid = VaadinSession.getCurrent().getAttribute("userId");
        if (uid == null) {
            UI.getCurrent().navigate("");
            return null;
        }
        return Integer.parseInt(uid.toString());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        bidIdStr = event.getRouteParameters().get("bidId").orElse(null);
        if (bidIdStr == null) {
            event.rerouteToError(NotFoundException.class);
            return;
        }
        try {
            bidId = Integer.parseInt(bidIdStr);
        } catch (NumberFormatException e) {
            event.rerouteToError(NotFoundException.class);
            return;
        }
        loadBid();
        buildPage();
    }

    private void loadBid() {
        String token = (String)VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("Please log in to view this bid", 3000, Position.MIDDLE);
            bid = null;
            return;
        }
        String url = BID_API_URL + "/" + bidId + "?authToken=" + token;
        try {
            ResponseEntity<BidRecieptDTO> resp = rest.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                BidRecieptDTO.class
            );
            bid = resp.getStatusCode().is2xxSuccessful() ? resp.getBody() : null;
        } catch (Exception e) {
            Notification.show("Error loading bid", 3000, Position.MIDDLE);
            bid = null;
        }
    }

    private void buildPage() {
        removeAll();

        if (bid == null) {
            add(new H2("Bid not found or could not be loaded."));
            return;
        }

        // ─── card container ───────────────────────────────────────────────
        VerticalLayout card = new VerticalLayout();
        card.addClassName("view-card");
        card.setSizeFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
            .set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)");

        // ─── read-only fields ────────────────────────────────────────────
        storeNameField.setReadOnly(true);
        storeNameField.setValue(getShopName(bid.getStoreId()));

        itemNameField.setReadOnly(true);
        itemNameField.setValue(getItemName(bid.getStoreId(), bid));

        userNameField.setReadOnly(true);
        userNameField.setValue(getUserName(bid.getUserId()));

        highestBidField.setReadOnly(true);
        highestBidField.setValue(String.valueOf(bid.getHighestBid()));

        completedField.setReadOnly(true);
        completedField.setValue(bid.isCompleted() ? "Yes" : "No");

        FormLayout form = new FormLayout(
            storeNameField,
            itemNameField,
            userNameField,
            highestBidField,
            completedField
        );
        card.add(form);

        // ─── your-bid controls ───────────────────────────────────────────
        if (bid.isCompleted()) {
            newBid.setReadOnly(true);
            placeBidButton.setEnabled(false);
            card.add(new H2("This bid is closed."));
        } else {
            newBid.setMin(1);
            newBid.setPlaceholder("Enter your bid");
            placeBidButton.addClickListener(e -> onPlaceBid());
            card.add(newBid, placeBidButton);
        }

        // ─── finalize layout ──────────────────────────────────────────────
        add(card);
        expand(card);
    }

    private void onPlaceBid() {
        String token = (String)VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("Login required", 3000, Position.MIDDLE);
            return;
        }
        
        int your = Optional.ofNullable(newBid.getValue()).orElse(0);
        double current = bid.getHighestBid();
        boolean amOwner = false;
        try {
            String url = apiBase
                + "/users/shops/" + bid.getStoreId()
                + "/owner?token=" + token;
            ResponseEntity<Integer> resp = rest.exchange(
                url, HttpMethod.GET, HttpEntity.EMPTY, Integer.class
            );
            amOwner = resp.getStatusCode().is2xxSuccessful()
                    && Objects.equals(getUserId(), resp.getBody());
        } catch (Exception ignored) {}
        if (amOwner) {
            if (your <= current) {
                Notification.show("As store manager, your bid must exceed the current highest bid (" + current + ")",3000, Position.MIDDLE);
                return;
            }
        } else {
            if (your >= current) {
                Notification.show("Your bid must be lower than the current highest bid (" + current + ")", 3000, Position.MIDDLE);
                return;
            }
        }
        String postUrl = BID_API_URL
                       + "/" + bidId
                       + "/offers?authToken=" + token
                       + "&bidPrice=" + your;

        ResponseEntity<Void> resp = rest.exchange(
            postUrl, HttpMethod.POST,
            new HttpEntity<String>(null, new HttpHeaders() {{
                setContentType(MediaType.APPLICATION_JSON);
            }}),

            Void.class
        );
        if (resp.getStatusCode().is2xxSuccessful()) {
            Notification.show("Bid placed successfully!", 2000, Position.MIDDLE);
            loadBid();
            buildPage();
            UI.getCurrent().navigate("bids");
        } else {
            Notification.show("Failed to place bid", 3000, Position.MIDDLE);
        }
    }

    // ─── helper methods unchanged ─────────────────────────────────────
    private String getShopName(int shopId) {
        String token = (String)VaadinSession.getCurrent().getAttribute("authToken");
        String url = apiBase + "/shops/" + shopId + "?token=" + token;
        ResponseEntity<JsonNode> resp = rest.exchange(
            url, HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            JsonNode.class
        );
        return (resp.getStatusCode().is2xxSuccessful() && resp.getBody()!=null)
             ? resp.getBody().path("name").asText("")
             : "";
    }

    private String getUserName(int userId) {
        String token = (String)VaadinSession.getCurrent().getAttribute("authToken");
        String url = apiBase + "/users/" + userId + "?token=" + token;
        ResponseEntity<JsonNode> resp = rest.exchange(
            url, HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            JsonNode.class
        );
        return (resp.getStatusCode().is2xxSuccessful() && resp.getBody()!=null)
             ? resp.getBody().path("username").asText("")
             : "";
    }

    private String getItemName(int shopId, BidRecieptDTO bid) {
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
