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
import jakarta.annotation.PostConstruct;

/**
 * Detailed auction view: shows details and post-auction dialog.
 */
@Route(value = "auction/:auctionId", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class AuctionDetailView extends VerticalLayout implements BeforeEnterObserver {
    private String auctionIdStr; // raw path parameter
    private int auctionId; // parsed integer ID
    private BidRecieptDTO bid; // loaded DTO

    private final RestTemplate rest = new RestTemplate();

    @Value("${url.api}")
    private String apiBase;

    private String AUCTION_API_URL;

    // Read‐only fields for auction details
    private final TextField purchaseIdField = new TextField("Bid ID");
    private final TextField storeIdField = new TextField("Store ID");
    private final TextField userIdField = new TextField("Owner ID");
    private final TextField highestBidderField = new TextField("highest Bidder ID");
    private final TextField highestBidField = new TextField("Highest Bid");
    private final TextField completedField = new TextField("Completed");

    // Controls for placing a new offer if the bid is open
    private final IntegerField newBidAmount = new IntegerField("Your Bid Amount");
    private final com.vaadin.flow.component.button.Button placeBidButton = new com.vaadin.flow.component.button.Button(
            "Place Auction");
    // private final com.vaadin.flow.component.button.Button finalizeBidButton = new com.vaadin.flow.component.button.Button(
    //         "Finalize Bid");

    @PostConstruct
    private void init() {
        AUCTION_API_URL = apiBase + "/purchases/auctions";
    }

    public AuctionDetailView() {
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
        // 1) Extract raw "auctionId" from URL
        auctionIdStr = event.getRouteParameters().get("auctionId").orElse(null);
        if (auctionIdStr == null) {
            event.rerouteToError(NotFoundException.class);
            return;
        }

        // 2) Parse to integer
        try {
            auctionId = Integer.parseInt(auctionIdStr);
            if (auctionId < 0) {
                removeAll();
                add(new H2("Invalid Auction ID"));
                return;
            }
        } catch (NumberFormatException ex) {
            removeAll();
            add(new H2("Invalid Auction ID"));
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

        String url = AUCTION_API_URL + "/" + auctionId + "?authToken=" + token;
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
                Notification.show("⚠️ Failed to load auction",
                        3000, Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            bid = null;
            Notification.show("❗ Error loading auction",
                    3000, Notification.Position.MIDDLE);
        }
    }

    private void buildPage() {
        removeAll();

        if (bid == null) {
            add(new H2("Auction not found or could not be loaded."));
            return;
        }

        // 1) Header
        H2 header = new H2("Auction Details — ID: " + auctionId);
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
            // finalizeBidButton.setEnabled(false);
            add(new H2("This auction is closed."));
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
            Notification.show("You must be logged in to place a auction.",
                    3000, Notification.Position.MIDDLE);
            return;
        }

        Integer amount = Optional.ofNullable(newBidAmount.getValue()).orElse(0);
        if (amount < 1) {
            Notification.show("Please enter a valid bid amount (≥ 1).",
                    3000, Notification.Position.MIDDLE);
            return;
        }

        String postUrl = AUCTION_API_URL
                + "/" + auctionId
                + "/offers?authToken=" + token
                + "&auctionAmount=" + amount;

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

}