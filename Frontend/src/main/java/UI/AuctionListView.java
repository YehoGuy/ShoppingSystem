package UI;

import DTOs.BidRecieptDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Route(value = "auctions", layout = AppLayoutBasic.class)
@AnonymousAllowed
@JsModule("./notification-client.js")
public class AuctionListView extends BaseView {

    private final RestTemplate rest = new RestTemplate();
    private final Grid<BidRecieptDTO> auctionGrid = new Grid<>(BidRecieptDTO.class, false);
    private final String apiBase;
    private final String auctionsEndpoint;

    public AuctionListView(@Value("${url.api}") String apiBase) {
        super("Auctions", "Browse and bid", "🔨", "➡️");
        this.apiBase         = apiBase;
        this.auctionsEndpoint = apiBase + "/purchases/auctions";

        // Make this view fill the browser
        setSizeFull();

        // ---- wrap your grid in a card ----
        H2 title = new H2("Available Auctions");
        title.getStyle().set("margin-bottom", "1rem");

        auctionGrid.setWidthFull();

        VerticalLayout card = new VerticalLayout(title, auctionGrid);
        card.addClassName("view-card");
        card.setSizeFull();              // card now has 100% height of its parent
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
            .set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)");

        // tell the card “give all remaining height to the grid”
        card.expand(auctionGrid);

        // ---- add & expand the card itself ----
        add(card);
        expand(card);

        // configure columns & load data
        configureGrid();
        fetchAllAuctions();
    }


    private void configureGrid() {
        auctionGrid.addColumn(dto -> getShopName(dto.getStoreId()))
                   .setHeader("Store Name")
                   .setAutoWidth(true);

        auctionGrid.addColumn(dto -> getItemName(dto.getStoreId(), dto))
                   .setHeader("Item Name")
                   .setAutoWidth(true);

        auctionGrid.addColumn(dto -> getUserName(dto.getUserId()))
                   .setHeader("Owner")
                   .setAutoWidth(true);

        auctionGrid.addColumn(BidRecieptDTO::getPrice)
                   .setHeader("Initial Price")
                   .setAutoWidth(true);

        auctionGrid.addColumn(BidRecieptDTO::getHighestBid)
                   .setHeader("Highest Bid")
                   .setAutoWidth(true);

        auctionGrid.addColumn(dto -> dto.isCompleted() ? "Yes" : "No")
                   .setHeader("Completed")
                   .setAutoWidth(true);

        // “Add Offer” button column
        auctionGrid.addColumn(new ComponentRenderer<Button, BidRecieptDTO>(dto -> {
            Button btn = new Button("Add Offer");
            btn.addClickListener(e -> {
                Integer me = getUserId();
                if (me != null && me.equals(dto.getUserId())) {
                    Notification.show("You cannot place a bid on your own auction", 3000, Position.MIDDLE);
                } else {
                    UI.getCurrent().navigate("auction/" + dto.getPurchaseId());
                }
            });
            if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
                btn.setVisible(false);
            }
            return btn;
        }))
        .setHeader("Auction")
        .setAutoWidth(true);
        

        // “Time Left” column with 1s polling
        auctionGrid.addColumn(new ComponentRenderer<>(dto -> {
            Span timer = new Span();
            Runnable update = () -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime end = dto.getEndTime();
                if (end == null) {
                    timer.setText("—");
                } else if (end.isBefore(now)) {
                    timer.setText("Ended");
                } else {
                    Duration d = Duration.between(now, end);
                    timer.setText(String.format(
                        "%02d:%02d:%02d",
                        d.toHours(), 
                        d.toMinutesPart(), 
                        d.toSecondsPart()
                    ));
                }
            };
            update.run();
            UI ui = UI.getCurrent();
            ui.setPollInterval(1000);
            ui.addPollListener(e -> update.run());
            return timer;
        })).setHeader("Time Left")
          .setAutoWidth(true);
    }

    private void fetchAllAuctions() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            add(new Text("You must log in to view auctions."));
            return;
        }
        String url = auctionsEndpoint + "?authToken=" + token;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<List<BidRecieptDTO>> resp = rest.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {}
        );

        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            auctionGrid.setItems(resp.getBody());
        } else {
            add(new Text("Failed to load auctions"));
        }
    }

    private Integer getUserId() {
        Object uid = VaadinSession.getCurrent().getAttribute("userId");
        if (uid != null) {
            return Integer.parseInt(uid.toString());
        }
        UI.getCurrent().navigate("");
        return null;
    }

    private String getShopName(int shopId) {
        try {
            ResponseEntity<JsonNode> resp = rest.exchange(
                apiBase + "/shops/" + shopId + "?token=" + VaadinSession.getCurrent().getAttribute("authToken"),
                HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class
            );
            return resp.getBody() != null
                ? resp.getBody().path("name").asText("")
                : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getUserName(int userId) {
        try {
            ResponseEntity<JsonNode> resp = rest.exchange(
                apiBase + "/users/" + userId + "?token=" + VaadinSession.getCurrent().getAttribute("authToken"),
                HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class
            );
            return resp.getBody() != null
                ? resp.getBody().path("username").asText("")
                : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getItemName(int shopId, BidRecieptDTO bid) {
        try {
            ResponseEntity<JsonNode> resp = rest.exchange(
                apiBase + "/shops/" + shopId + "/items?token=" + VaadinSession.getCurrent().getAttribute("authToken"),
                HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class
            );
            if (resp.getBody() != null) {
                for (JsonNode item : resp.getBody()) {
                    if (bid.getItems().containsKey(item.path("id").asInt(-1))) {
                        return item.path("name").asText("");
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }
        return "";
    }
}
