package UI;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import DTOs.BidRecieptDTO;

/**
 * Route: /bid/{bidId}
 *
 * Mirrors CreateBidView’s pattern: implements BeforeEnterObserver,
 * extracts "bidId", loads bid, and renders UI without any finalize logic.
 */
@Route(value = "bid/:bidId", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class BidDetailView extends VerticalLayout implements BeforeEnterObserver {

    private String bidIdStr;
    private int bidId;
    private BidRecieptDTO bid;

    private final RestTemplate rest = new RestTemplate();
    private final String apiBase;
    private final String BID_API_URL;

    private final TextField purchaseIdField = new TextField("Bid ID");
    private final TextField storeIdField = new TextField("Store ID");
    private final TextField userIdField = new TextField("Owner ID");
    private final TextField highestBidderField = new TextField("Highest Bidder ID");
    private final TextField highestBidField = new TextField("Highest Bid");
    private final TextField completedField = new TextField("Completed");

    private final IntegerField newBid = new IntegerField("Your Bid");
    private final com.vaadin.flow.component.button.Button placeBidButton = new com.vaadin.flow.component.button.Button(
            "Place Bid");

    public BidDetailView(@Value("${url.api}") String apiBase) {
        this.apiBase = apiBase;
        this.BID_API_URL = this.apiBase + "/purchases/bids";

        getUserId(); // Ensure userId is set
        setPadding(true);
        setSpacing(true);
    }

    private Integer getUserId() {
        Object attr = VaadinSession.getCurrent().getAttribute("userId");
        if (attr == null) {
            UI.getCurrent().navigate("");
            return null;
        }
        return Integer.parseInt(attr.toString());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        bidIdStr = event.getRouteParameters().get("bidId").orElse(null);
        if (bidIdStr == null || bidIdStr.isBlank()) {
            event.rerouteToError(NotFoundException.class);
            return;
        }

        try {
            bidId = Integer.parseInt(bidIdStr);
            if (bidId < 0) {
                removeAll();
                add(new H2("Invalid Bid ID"));
                return;
            }
        } catch (NumberFormatException ex) {
            removeAll();
            add(new H2("Invalid Bid ID"));
            return;
        }

        loadBid();
        buildPage();
    }

    private void loadBid() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("You must be logged in to view this page.", 3000, Position.MIDDLE);
            bid = null;
            return;
        }

        String url = BID_API_URL + "/" + bidId + "?authToken=" + token;
        try {
            ResponseEntity<BidRecieptDTO> resp = rest.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), BidRecieptDTO.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                bid = resp.getBody();
            } else {
                bid = null;
                Notification.show("⚠️ Failed to load bid", 3000, Position.MIDDLE);
            }
        } catch (Exception e) {
            bid = null;
            Notification.show("❗ Error loading bid", 3000, Position.MIDDLE);
        }
    }

    private void buildPage() {
        removeAll();

        if (bid == null) {
            add(new H2("Bid not found or could not be loaded."));
            return;
        }

        H2 header = new H2("Bid Details — ID: " + bidId);
        add(header);

        purchaseIdField.setReadOnly(true);
        purchaseIdField.setValue(String.valueOf(bid.getPurchaseId()));
        storeIdField.setReadOnly(true);
        storeIdField.setValue(String.valueOf(bid.getStoreId()));
        userIdField.setReadOnly(true);
        userIdField.setValue(String.valueOf(bid.getThisBidderId()));
        highestBidderField.setReadOnly(true);
        highestBidderField.setValue(String.valueOf(bid.getHighestBidderId()));
        highestBidField.setReadOnly(true);
        highestBidField.setValue(String.valueOf(bid.getHighestBid()));
        completedField.setReadOnly(true);
        completedField.setValue(bid.isCompleted() ? "Yes" : "No");

        FormLayout form = new FormLayout(
                purchaseIdField,
                storeIdField,
                userIdField,
                highestBidderField,
                highestBidField,
                completedField);
        add(form);

        if (bid.isCompleted()) {
            newBid.setReadOnly(true);
            placeBidButton.setEnabled(false);
            add(new H2("This bid is closed."));
            return;
        }

        newBid.setMin(1);
        newBid.setPlaceholder("Enter your bid");
        placeBidButton.addClickListener(e -> onPlaceBid());
        add(newBid, placeBidButton);
    }

    private void onPlaceBid() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("You must be logged in to place a bid.", 3000, Position.MIDDLE);
            return;
        }

        Integer price = Optional.ofNullable(newBid.getValue()).orElse(0);
        if (price < bid.getHighestBid()){
            Notification.show("Your bid must be higher than the current highest bid.", 3000, Position.MIDDLE);
        }

        String postUrl = BID_API_URL + "/" + bidId + "/offers?authToken=" + token + "&bidPrice=" + price;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> resp = rest.exchange(postUrl, HttpMethod.POST, entity, Void.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                Notification.show("Bid placed successfully!", 2000, Position.MIDDLE);
                loadBid();
                buildPage();
            } else {
                Notification.show("Failed to place bid", 3000, Position.MIDDLE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Error placing bid", 4000, Position.MIDDLE);
        }
    }
}
