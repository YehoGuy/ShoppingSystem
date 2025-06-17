package UI;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
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
 * This version mirrors CreateBidView’s pattern: it implements
 * BeforeEnterObserver,
 * extracts the "bidId" path parameter as a string, parses it to int, then calls
 * loadBid() and buildPage() to render the UI.
 */
@Route(value = "bid/:bidId", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class BidDetailView extends VerticalLayout implements BeforeEnterObserver {

    private String bidIdStr; // raw path parameter
    private int bidId; // parsed integer ID
    private BidRecieptDTO bid; // loaded DTO

    private final RestTemplate rest = new RestTemplate();

    private final String apiBase;

    private final String BID_API_URL;

    // Read‐only fields for bid details
    private final TextField purchaseIdField = new TextField("Bid ID");
    private final TextField storeIdField = new TextField("Store ID");
    private final TextField userIdField = new TextField("Owner ID");
    private final TextField highestBidderField = new TextField("highest Bidder ID");
    private final TextField highestBidField = new TextField("Highest Bid");
    private final TextField completedField = new TextField("Completed");

    // Controls for placing a new offer if the bid is open
    private final IntegerField newBidAmount = new IntegerField("Your Bid Amount");
    private final com.vaadin.flow.component.button.Button placeBidButton = new com.vaadin.flow.component.button.Button(
            "Place Bid");
    private final com.vaadin.flow.component.button.Button finalizeBidButton = new com.vaadin.flow.component.button.Button(
            "Finalize Bid");

    public BidDetailView(@Value("${url.api}") String apiBase) {
        this.apiBase    = apiBase;
        this.BID_API_URL = this.apiBase + "/purchases/bids";

        getUserId(); // Ensure userId is set in session
        setPadding(true);
        setSpacing(true);
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // 1) Extract raw "bidId" from URL
        bidIdStr = event.getRouteParameters().get("bidId").orElse(null);
        if (bidIdStr == null || bidIdStr.trim().isEmpty()) {
            event.rerouteToError(NotFoundException.class);
            return;
        }

        // 2) Parse to integer
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

        // 3) Load bid data and build UI
        loadBid();
        buildPage();
    }

    private void loadBid() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("You must be logged in to view this page.",
                    3000, Notification.Position.MIDDLE);
            bid = null;
            return;
        }

        String url = BID_API_URL + "/" + bidId + "?authToken=" + token;
        try {
            ResponseEntity<BidRecieptDTO> resp = rest.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    BidRecieptDTO.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                bid = resp.getBody();
            } else if (resp.getStatusCode() == HttpStatus.NOT_FOUND) {
                bid = null;
                // Keep bid null; buildPage() will show “not found”
            } else {
                bid = null;
                Notification.show("⚠️ Failed to load bid",
                        3000, Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            bid = null;
            Notification.show("❗ Error loading bid",
                    3000, Notification.Position.MIDDLE);
        }
    }

    private void buildPage() {
        removeAll();

        if (bid == null) {
            add(new H2("Bid not found or could not be loaded."));
            return;
        }

        // 1) Header
        H2 header = new H2("Bid Details — ID: " + bidId);
        add(header);

        // 2) Populate read‐only fields
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

        Integer currentUserId = (Integer) VaadinSession.getCurrent().getAttribute("userId");
        if (bid.getUserId() != currentUserId) {
            finalizeBidButton.setEnabled(false);
            finalizeBidButton.setVisible(false);
        } else {
            finalizeBidButton.setText("Finalize Bid");
            finalizeBidButton.addClickListener(listener -> onFinalizeBid());
            add(finalizeBidButton);
        }

        // 3) FormLayout for the six fields
        FormLayout form = new FormLayout();
        form.add(
                purchaseIdField,
                storeIdField,
                userIdField,
                highestBidderField,
                highestBidField,
                completedField);
        add(form);

        // 4) If bid is completed, show message and do not add new‐bid controls
        if (bid.isCompleted()) {
            newBidAmount.setReadOnly(true);
            placeBidButton.setEnabled(false);
            finalizeBidButton.setEnabled(false);
            add(new H2("This bid is closed."));
            return;
        }

        // 5) Otherwise, add NumberField + Button for placing new bid
        newBidAmount.setMin(1);
        newBidAmount.setPlaceholder("Enter your amount");
        newBidAmount.setWidth("120px");
        placeBidButton.addClickListener(e -> onPlaceBid());
        add(newBidAmount, placeBidButton);
    }

    private void onPlaceBid() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("You must be logged in to place a bid.",
                    3000, Notification.Position.MIDDLE);
            return;
        }

        Integer amount = Optional.ofNullable(newBidAmount.getValue()).orElse(0);
        if (amount < 1) {
            Notification.show("Please enter a valid bid amount (≥ 1).",
                    3000, Notification.Position.MIDDLE);
            return;
        }

        String postUrl = BID_API_URL
                + "/" + bidId
                + "/offers?authToken=" + token
                + "&bidAmount=" + amount;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> resp = rest.exchange(
                    postUrl,
                    HttpMethod.POST,
                    entity,
                    Void.class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                Notification.show("Bid placed successfully!",
                        2000, Notification.Position.MIDDLE);
                // Re‐load bid and rebuild UI to update Highest Bid
                loadBid();
                buildPage();
            } else {
                Notification.show("Failed to place bid",
                        3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Error placing bid",
                    4000, Notification.Position.MIDDLE);
        }
    }

    private void onFinalizeBid() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            Notification.show("You must log in…");
            return;
        }
        // Build the finalize URL, e.g.:
        String finalizeUrl = BID_API_URL
                + "/" + bidId + "/finalize?authToken=" + token;
        try {
            ResponseEntity<Void> resp = rest.exchange(
                    finalizeUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(new HttpHeaders()),
                    Void.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                Notification.show("Bid finalized!", 2000, Notification.Position.MIDDLE);
                // Re‐load the bid from the server to reflect that completed = true
                loadBid();
                buildPage();
            } else {
                Notification.show("Error finalizing bid",
                        3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("Error finalizing bid",
                    3000, Notification.Position.MIDDLE);
        }
    }

    /*
     * private void onFinalizeBid() {
     * String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
     * if (token == null || token.isBlank()) {
     * Notification.show("You must be logged in to finalize a bid.",
     * 3000, Notification.Position.MIDDLE);
     * return;
     * }
     * 
     * String finalizeUrl = BID_API_URL + "/" + bidId + "/finalize?authToken=" +
     * token;
     * 
     * try {
     * HttpHeaders headers = new HttpHeaders();
     * headers.setContentType(MediaType.APPLICATION_JSON);
     * HttpEntity<Void> entity = new HttpEntity<>(headers);
     * 
     * ResponseEntity<Void> resp = rest.exchange(
     * finalizeUrl,
     * HttpMethod.POST,
     * entity,
     * Void.class
     * );
     * if (resp.getStatusCode().is2xxSuccessful()) {
     * Notification.show("Bid finalized successfully!",
     * 2000, Notification.Position.MIDDLE);
     * // Re‐load bid and rebuild UI to show it as completed
     * loadBid();
     * buildPage();
     * } else {
     * Notification.show("Failed to finalize bid: " + resp.getStatusCode(),
     * 3000, Notification.Position.MIDDLE);
     * }
     * } catch (Exception ex) {
     * ex.printStackTrace();
     * Notification.show("Error finalizing bid: " + ex.getMessage(),
     * 4000, Notification.Position.MIDDLE);
     * }
     * }
     */
}
