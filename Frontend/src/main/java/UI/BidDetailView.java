package UI;

import java.util.List;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import DTOs.BidRecieptDTO;

/**
 * This view is bound to the route "bid/:bidId" so that Vaadin injects the bidId from the URL.
 */
@Route(value = "bid/:bidId", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class BidDetailView extends VerticalLayout implements HasUrlParameter<Integer> {

    private int bidId;
    private final RestTemplate restTemplate = new RestTemplate();
    private BidRecieptDTO bidDetail;

    // Read‐only fields to display bid data
    private final TextField purchaseIdField = new TextField("Bid ID");
    private final TextField storeIdField    = new TextField("Store ID");
    private final TextField userIdField     = new TextField("Owner ID");
    private final TextField priceField      = new TextField("Current Price");
    private final TextField highestBidField = new TextField("Highest Bid");
    private final TextField completedField  = new TextField("Completed");

    // Controls to place a new bid
    private final IntegerField newBidAmount    = new IntegerField("Your Bid Amount");
    private final Button        placeBidButton = new Button("Place Bid");

    private static final String FETCH_ALL_BIDS_URL = "http://localhost:8080/api/purchases/bids";

    public BidDetailView() {
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer parameter) {
        // Vaadin will call this with the integer extracted from the URL: /bid/{bidId}
        this.bidId = (parameter != null && parameter > 0) ? parameter : -1;
        if (bidId <= 0) {
            add(new H2("Invalid Bid ID"));
            return;
        }
        fetchBidDetailsAndBuildLayout();
    }

    private void fetchBidDetailsAndBuildLayout() {
        try {
            // 1. Read authToken from session
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            if (authToken == null || authToken.isBlank()) {
                add(new H2("You must be logged in to view bid details."));
                return;
            }

            // 2. Call GET /api/purchases/bids?authToken=<token>
            String urlWithToken = FETCH_ALL_BIDS_URL + "?authToken=" + authToken;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<BidRecieptDTO>> response = restTemplate.exchange(
                    urlWithToken,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<BidRecieptDTO> allBids = response.getBody();
                Optional<BidRecieptDTO> maybeBid = allBids.stream()
                        .filter(dto -> dto.getPurchaseId() == bidId)
                        .findFirst();

                if (maybeBid.isEmpty()) {
                    add(new H2("Bid ID " + bidId + " not found."));
                    return;
                }

                bidDetail = maybeBid.get();
                fillFormWithBid(bidDetail);
                buildLayout();
            } else {
                add(new H2("Failed to load bids: " + response.getStatusCode()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            add(new H2("Error fetching bid details: " + ex.getMessage()));
        }
    }

    private void fillFormWithBid(BidRecieptDTO dto) {
        purchaseIdField.setReadOnly(true);
        purchaseIdField.setValue(String.valueOf(dto.getPurchaseId()));

        storeIdField.setReadOnly(true);
        storeIdField.setValue(String.valueOf(dto.getStoreId()));

        userIdField.setReadOnly(true);
        userIdField.setValue(String.valueOf(dto.getThisBidderId()));

        priceField.setReadOnly(true);
        priceField.setValue(String.valueOf(dto.getPrice()));

        highestBidField.setReadOnly(true);
        highestBidField.setValue(String.valueOf(dto.getHighestBid()));

        completedField.setReadOnly(true);
        completedField.setValue(dto.isCompleted() ? "Yes" : "No");
    }

    private void buildLayout() {
        H2 header = new H2("Bid Details — ID: " + bidId);
        add(header);

        FormLayout form = new FormLayout();
        form.add(
            purchaseIdField,
            storeIdField,
            userIdField,
            priceField,
            highestBidField,
            completedField
        );
        add(form);

        newBidAmount.setMin(1);
        newBidAmount.setPlaceholder("Enter your bid (integer)");

        placeBidButton.addClickListener(evt -> {
            Integer amount = newBidAmount.getValue();
            if (amount == null) {
                Notification.show("Please enter a bid amount.");
                return;
            }
            submitNewBid(amount);
        });

        add(newBidAmount, placeBidButton);
    }

    private void submitNewBid(int amount) {
        try {
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            if (authToken == null || authToken.isBlank()) {
                Notification.show("You must be logged in to place a bid.", 3000, Notification.Position.MIDDLE);
                return;
            }

            String postUrl = "http://localhost:8080/api/purchases/bids/"
                           + bidId
                           + "/offers?authToken="
                           + authToken
                           + "&bidAmount="
                           + amount;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    postUrl,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            if (response.getStatusCode() == HttpStatus.ACCEPTED ||
                response.getStatusCode() == HttpStatus.OK) {
                Notification.show("Bid placed successfully!", 2000, Notification.Position.MIDDLE);
                fetchBidDetailsAndBuildLayout(); // refresh data
            } else {
                Notification.show("Failed to place bid: " + response.getStatusCode(), 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Error placing bid: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }
}
