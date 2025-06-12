package UI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.vaadin.flow.component.button.Button;

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
    private final TextField storeNameField = new TextField("Store Name");
    private final TextField itemNameField = new TextField("Item Name");
    private final TextField userNameField = new TextField("Owner Name");
    private final TextField highestBidField = new TextField("Highest Bid");
    private final TextField completedField = new TextField("Completed");

    // Controls for placing a new offer if the bid is open
    private final IntegerField newBidAmount = new IntegerField("Your Bid Amount");
    private final Button placeBidButton = new Button("Place Auction-Bid");

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
        H2 header = new H2("Auction Details");
        add(header);

        // 2) Populate read‐only fields
        storeNameField.setReadOnly(true);
        int shopId = bid.getStoreId();
        //Http request from ShopController to extract the store' name
        String shopName = getShopName(shopId);
        storeNameField.setValue(String.valueOf(shopName));

        itemNameField.setReadOnly(true);
        //Http request from ShopController to extract the item' name
        String itemName = getItemName(shopId, bid);
        itemNameField.setValue(itemName);

        userNameField.setReadOnly(true);
        int userId = bid.getUserId();
        //Http request from UserController to extract the user' name
        String userName = getUserName(userId);
        userNameField.setValue(String.valueOf(userName));

        System.out.println("Highest bid: " + bid.getHighestBidderId());

        highestBidField.setReadOnly(true);
        highestBidField.setValue(String.valueOf(bid.getHighestBid()));

        completedField.setReadOnly(true);
        completedField.setValue(bid.isCompleted() ? "Yes" : "No");

        // 3) FormLayout for the six fields
        FormLayout form = new FormLayout();
        form.add(
                storeNameField,
                itemNameField,
                userNameField,
                highestBidField,
                completedField);
        add(form);

        // 4) If bid is completed, show message and do not add new‐bid controls
        if (bid.isCompleted()) {
            newBidAmount.setReadOnly(true);
            placeBidButton.setEnabled(false);
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

        String postUrl = AUCTION_API_URL
                + "/" + auctionId
                + "/offers?authToken=" + token
                + "&bidAmount=" + newBidAmount.getValue();

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


    private String getShopName(int shopId) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            Notification.show("Not authenticated", 3000, Notification.Position.MIDDLE);
            return "";
        }

        String url = apiBase + "/shops/" + shopId + "?token=" + token;
        try {
            ResponseEntity<JsonNode> resp = rest.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                JsonNode.class
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return resp.getBody().path("name").asText("");
            }
        } catch (Exception e) {
            Notification.show("Error loading shop", 3000, Notification.Position.MIDDLE);
        }
        return "";
    }

    private String getUserName(int userId) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            Notification.show("Not authenticated", 3000, Notification.Position.MIDDLE);
            return "";
        }

        String url = apiBase + "/users/" + userId + "?token=" + token;
        try {
            ResponseEntity<JsonNode> resp = rest.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                JsonNode.class
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return resp.getBody().path("username").asText("");
            }
        } catch (Exception e) {
            Notification.show("Error loading user", 3000, Notification.Position.MIDDLE);
        }
        return "";
    }

    private String getItemName(int shopId, BidRecieptDTO bid) {
        // grab your auth token
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("Not authenticated", 3000, Notification.Position.MIDDLE);
            return "";
        }

        // call your ShopController’s “list items” endpoint
        String url = apiBase + "/shops/" + shopId + "/items?token=" + token;
        try {
            ResponseEntity<JsonNode> resp = rest.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                JsonNode.class
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode itemsArray = resp.getBody();
                // iterate over returned ItemDTOs
                for (JsonNode itemNode : itemsArray) {
                    int id = itemNode.path("id").asInt(-1);
                    // if this auction’s map contained that itemId, return its name
                    if (bid.getItems().containsKey(id)) {
                        return itemNode.path("name").asText("");
                    }
                }
            }
        } catch (Exception e) {
            Notification.show("Error loading item", 3000, Notification.Position.MIDDLE);
        }

        // fallback if nothing was found
        return "";
    }


}