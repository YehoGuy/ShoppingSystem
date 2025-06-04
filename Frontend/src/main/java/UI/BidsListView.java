// ───────────────────────────────────────────────────────────────────────────
// src/main/java/UI/BidsListView.java
// Customer‐facing screen: Show a table of all bids, and navigate
// to details when a row is clicked.
// ───────────────────────────────────────────────────────────────────────────
package UI;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Value;


import DTOs.BidRecieptDTO;
import jakarta.annotation.PostConstruct;

@Route(value = "bids", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class BidsListView extends VerticalLayout {

    @Value("${url.api}/purchases/bids")
    private String BASE_URL;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Grid<BidRecieptDTO> bidGrid = new Grid<>(BidRecieptDTO.class, false);

    public BidsListView() {
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Available Bids");
        add(header);

        // Configure the columns you want to show
        bidGrid.addColumn(BidRecieptDTO::getPurchaseId)
               .setHeader("Bid ID")
               .setAutoWidth(true);
        bidGrid.addColumn(BidRecieptDTO::getStoreId)
               .setHeader("Store ID")
               .setAutoWidth(true);
        bidGrid.addColumn(dto -> dto.getPrice())
               .setHeader("Current Price")
               .setAutoWidth(true);
        bidGrid.addColumn(dto -> dto.getHighestBid())
               .setHeader("Highest Bid")
               .setAutoWidth(true);
        bidGrid.addColumn(dto -> dto.isCompleted() ? "Yes" : "No")
               .setHeader("Completed");

        add(bidGrid);

        // Add a listener so that when a row is clicked, we navigate to /bid/{purchaseId}
        bidGrid.asSingleSelect().addValueChangeListener(event -> {
            BidRecieptDTO selected = event.getValue();
            if (selected != null) {
                // This causes Vaadin to go to /bid/<selectedPurchaseId>
                getUI().ifPresent(ui -> ui.navigate("bid/" + selected.getPurchaseId()));
            }
        });

    }

    @PostConstruct
    private void init() {
        fetchAllBids();
    }

    private void fetchAllBids() {
        try {
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            if (authToken == null || authToken.isBlank()) {
                add(new Text("You must log in to view bids."));
                return;
            }

            // Call GET /api/purchases/bids?authToken=<token>
            String urlWithToken = BASE_URL + "?authToken=" + authToken;
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
                bidGrid.setItems(response.getBody());
            } else {
                add(new Text("Failed to load bids: " + response.getStatusCode()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            add(new Text("Error fetching bids: " + ex.getMessage()));
        }
    }
}