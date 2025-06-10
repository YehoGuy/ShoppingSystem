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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import DTOs.BidRecieptDTO;
import jakarta.annotation.PostConstruct;

/**
 * List all auctions with current status.
 */
@Route(value = "auctions", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class AuctionListView extends VerticalLayout {
    
    @Value("${url.api}/purchases/auctions")
    private String BASE_URL;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Grid<BidRecieptDTO> auctionGrid = new Grid<>(BidRecieptDTO.class, false);

    public AuctionListView() {
        getUserId(); // Ensure userId is set in session
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Available Auctions");
        add(header);

        // Configure the columns you want to show
        auctionGrid.addColumn(BidRecieptDTO::getPurchaseId)
                .setHeader("Bid ID")
                .setAutoWidth(true);
        auctionGrid.addColumn(BidRecieptDTO::getStoreId)
                .setHeader("Store ID")
                .setAutoWidth(true);
        auctionGrid.addColumn(dto -> dto.getPrice())
                .setHeader("Current Price")
                .setAutoWidth(true);
        auctionGrid.addColumn(dto -> dto.getHighestBid())
                .setHeader("Highest Bid")
                .setAutoWidth(true);
        auctionGrid.addColumn(dto -> dto.isCompleted() ? "Yes" : "No")
                .setHeader("Completed");

        add(auctionGrid);

        // Add a listener so that when a row is clicked, we navigate to
        // /auction/{purchaseId}
        auctionGrid.asSingleSelect().addValueChangeListener(event -> {
            BidRecieptDTO selected = event.getValue();
            if (selected != null) {
                // This causes Vaadin to go to /auction/<selectedPurchaseId>
                getUI().ifPresent(ui -> ui.navigate("auction/" + selected.getPurchaseId()));
            }
        });

    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    @PostConstruct
    private void init() {
        fetchAllAuctions();
    }

    private void fetchAllAuctions() {
        try {
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            if (authToken == null || authToken.isBlank()) {
                add(new Text("You must log in to view auctions."));
                return;
            }

            // Call GET /api/purchases/auctions?authToken=<token>
            String urlWithToken = BASE_URL + "?authToken=" + authToken;
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
}