package UI;

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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.notification.Notification;

import DTOs.BidRecieptDTO;

@Route(value = "auction/:auctionId", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class AuctionDetailView extends BaseView implements BeforeEnterObserver {

    private String auctionIdStr;        // raw path param
    private int auctionId;              // parsed
    private BidRecieptDTO bid;          // loaded DTO

    private final RestTemplate rest = new RestTemplate();
    private final String apiBase;
    private final String AUCTION_API_URL;

    // ─── fields ────────────────────────────────────────────────────────────────
    private final TextField storeNameField    = new TextField("Store Name");
    private final TextField itemNameField     = new TextField("Item Name");
    private final TextField userNameField     = new TextField("Owner Name");
    private final TextField highestBidField   = new TextField("Highest Bid");
    private final TextField completedField    = new TextField("Completed");

    private final IntegerField newBidAmount   = new IntegerField("Your Bid Amount");
    private final Button placeBidButton       = new Button("Place Auction-Bid");

    public AuctionDetailView(@Value("${url.api}") String apiBase) {
        // super(title, subtitle, leftIcon, rightIcon)
        super("Auction Details", "See & place bids", "🔨", "➡️");
        this.apiBase         = apiBase;
        this.AUCTION_API_URL = apiBase + "/purchases/auctions";

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        auctionIdStr = event.getRouteParameters().get("auctionId").orElse(null);
        if (auctionIdStr == null) {
            event.rerouteToError(NotFoundException.class);
            return;
        }
        try {
            auctionId = Integer.parseInt(auctionIdStr);
        } catch (NumberFormatException e) {
            event.rerouteToError(NotFoundException.class);
            return;
        }
        loadBid();
        buildPage();
    }

    private void loadBid() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("Please log in to view this auction", 3000, Notification.Position.MIDDLE);
            bid = null;
            return;
        }
        String url = AUCTION_API_URL + "/" + auctionId + "?authToken=" + token;
        ResponseEntity<BidRecieptDTO> resp = rest.exchange(
            url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), BidRecieptDTO.class
        );
        bid = resp.getStatusCode().is2xxSuccessful() ? resp.getBody() : null;
    }

    private void buildPage() {
        removeAll();

        if (bid == null) {
            add(new H2("Auction not found or failed to load."));
            return;
        }

        // ─── wrap content in a card ──────────────────────────────────────
        VerticalLayout card = new VerticalLayout();
        card.addClassName("view-card");
        card.setSizeFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
            .set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)");

        // ─── form with read-only fields ────────────────────────────────
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

        // ─── new-bid controls ─────────────────────────────────────────────
        if (bid.isCompleted()) {
            newBidAmount.setReadOnly(true);
            placeBidButton.setEnabled(false);
            card.add(new H2("This auction is closed."));
        } else {
            newBidAmount.setMin(1);
            newBidAmount.setPlaceholder("Enter your amount");
            newBidAmount.setWidth("120px");

            placeBidButton.addClickListener(e -> onPlaceBid());
            card.add(newBidAmount, placeBidButton);
        }

        // ─── finish layout ───────────────────────────────────────────────
        add(card);
        expand(card);
    }

    private void onPlaceBid() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("Login required", 3000, Notification.Position.MIDDLE);
            return;
        }
        int offer = Optional.ofNullable(newBidAmount.getValue()).orElse(0);
        if (offer <= bid.getHighestBid()) {
            Notification.show("Must be higher than current bid", 3000, Notification.Position.MIDDLE);
            return;
        }
        String url = AUCTION_API_URL
                   + "/" + auctionId
                   + "/offers?authToken=" + token
                   + "&bidAmount=" + offer;

        ResponseEntity<Void> resp = rest.exchange(
            url, HttpMethod.POST,
            new HttpEntity<String>(null, new HttpHeaders() {{
                setContentType(MediaType.APPLICATION_JSON);
            }}),
            Void.class
        );
        if (resp.getStatusCode().is2xxSuccessful()) {
            Notification.show("Bid placed!", 2000, Notification.Position.MIDDLE);
            loadBid();
            buildPage();
            UI.getCurrent().navigate("auctions");
        } else {
            Notification.show("Failed to place bid", 3000, Notification.Position.MIDDLE);
        }
    }

    // ─── helper methods copied unchanged from your original ───────────
    private String getShopName(int shopId) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
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
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
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
        String url = apiBase + "/shops/" + shopId + "/items?token=" + token;
        ResponseEntity<JsonNode> resp = rest.exchange(
            url, HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            JsonNode.class
        );
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody()!=null) {
            for (JsonNode item : resp.getBody()) {
                if (bid.getItems().containsKey(item.path("id").asInt(-1))) {
                    return item.path("name").asText("");
                }
            }
        }
        return "";
    }
}
