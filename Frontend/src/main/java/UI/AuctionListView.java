package UI;

import java.time.LocalDateTime;
import java.time.Duration;
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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.data.renderer.ComponentRenderer;

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
        auctionGrid.addColumn((BidRecieptDTO dto) -> dto.getPurchaseId())
                .setHeader("Auction ID")
                .setAutoWidth(true);
        auctionGrid.addColumn((BidRecieptDTO dto) -> dto.getStoreId())
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

        // add a button to add offer to auction
        auctionGrid.addColumn(new ComponentRenderer<>(dto -> {
        Button addOffer = new Button("Add Offer");
        addOffer.addClickListener(evt -> {
            Integer me = getUserId();
            if (me != null && me.equals(dto.getUserId())) {
                Notification.show(
                "You cannot place a bid on your own auction",
                3000,
                Position.MIDDLE
                );
            } else {
                UI.getCurrent().navigate("auction/" + dto.getPurchaseId());
            }
        });
        return addOffer;
        }))
        .setHeader("Actions")
        .setAutoWidth(true);
        
        // ─── Time left column ───────────────────────────────────────────────
        auctionGrid.addColumn(new ComponentRenderer<>(dto -> {
            Span timer = new Span();
            Runnable update = () -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime end = dto.getEndTime();
                if (end == null) {
                    timer.setText("—");           // no data
                } else if (end.isBefore(now)) {
                    timer.setText("Ended");
                } else {
                    Duration d = Duration.between(now, end);
                    timer.setText(String.format(
                        "%02d:%02d:%02d",
                        d.toHours(),
                        d.toMinutesPart(),
                        d.toSecondsPart()));
                }
            };
            update.run();
            UI ui = UI.getCurrent();
            ui.setPollInterval(1000);
            ui.addPollListener(e -> update.run());
            return timer;
        }))
        .setHeader("Time Left")
        .setAutoWidth(true);
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