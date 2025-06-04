package UI;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;                  // for Map.Entry

import DTOs.BidRecieptDTO;                 // if you use List elsewhere
import DTOs.ItemDTO;
import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;

@Route(value = "shop", layout = AppLayoutBasic.class)
@JsModule("./js/notification-client.js")
public class ShopView extends VerticalLayout implements HasUrlParameter<String>, BeforeEnterObserver {

    @Value("${url.api}/shops")
    private String SHOP_API_URL;    

    @Value("${url.api}/purchases/shops")
    private String PURCHASE_HISTORY_URL;

    private final RestTemplate restTemplate = new RestTemplate();
    private ShopDTO shop;
    private Map<ItemDTO, Double> prices;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }
        UI.getCurrent().getPage().executeJs("import(./js/notification-client.js).then(m -> m.connectNotifications($0))",
                getUserId());
    }

    private String getUserId() {
        return VaadinSession.getCurrent().getAttribute("userId").toString();
    }

    @Override
    public void setParameter(com.vaadin.flow.router.BeforeEvent event, @OptionalParameter String shopId) {
        if (shopId == null || shopId.isEmpty()) {
            add(new Span("❌ No shop ID provided."));
            return;
        }

        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String url = SHOP_API_URL + "/" + shopId + "?token=" + token;
        try {
            ResponseEntity<ShopDTO> resp = restTemplate.getForEntity(url, ShopDTO.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                shop = resp.getBody();
                prices = ShopDTO.itemPricesToMapConverter(shop.getItems(), shop.getItemPrices());
                buildPage();
            } else {
                Notification.show("⚠️ Failed to load shop: " + resp.getStatusCode());
            }
        } catch (Exception e) {
            Notification.show("❗ Error loading shop: " + e.getMessage());
        }
    }

    private void fetchStoreBids(Grid<BidRecieptDTO> shopBidsGrid) {
        try {
            // 1. Read the authToken from VaadinSession
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            if (authToken == null || authToken.isBlank()) {
                // If there's no token, do not call the endpoint; just show an empty grid
                shopBidsGrid.setItems(Collections.emptyList());
                return;
            }

            // 2. Build the full URL including the required query param "authToken"
            //
            // EXACTLY matches the backend mapping:
            //    @GetMapping("/shops/{shopId}/bids")
            //    public ResponseEntity<List<BidRecieptDTO>> getBidsForShop(
            //            @PathVariable int shopId,
            //            @RequestParam String authToken)
            //
            // Therefore we must call:
            //    GET /api/purchases/shops/{shopId}/bids?authToken=<token>
            //
            String url = PURCHASE_HISTORY_URL + "/" + shop.getShopId() + "/bids?authToken=" + authToken;


            // 3. Prepare headers (JSON)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 4. Make the GET call
            ResponseEntity<List<BidRecieptDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            // 5. If 200 OK, bind the response body (List<BidRecieptDTO>) to the grid
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                shopBidsGrid.setItems(response.getBody());
            } else {
                // If we get a 4xx or 5xx, show an error header
                add(new H2("Failed to load shop’s bids: " + response.getStatusCode()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            add(new H2("Error fetching shop’s bids: " + ex.getMessage()));
        }
    }


    /**
     * Renders the shop page with header, items, and reviews.
     */
    private void buildPage() {
        removeAll();
        setPadding(true);
        setSpacing(true);

        // Header with emoji
        H1 header = new H1("🛍️ Welcome to " + shop.getName());
        add(header);

        // Items section
        add(new H2("📦 Items"));
        VerticalLayout itemsLayout = new VerticalLayout();
        itemsLayout.setWidthFull();
        for (Map.Entry<ItemDTO, Integer> e : ShopDTO
                .itemQuantitiesToMapConverter(shop.getItems(), shop.getItemQuantities()).entrySet()) {
            ItemDTO item = e.getKey();
            int available = e.getValue();
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);

            Span name = new Span("🍽️ " + item.getName());
            Span priceSpan = new Span("💲 " + prices.getOrDefault(item, 0.0));
            Span stock = new Span("📊 In Stock: " + available);
            Button addBtn = new Button("🛒 Add to Cart", evt -> Notification.show("🚀 Added " + item.getName()));

            row.add(name, priceSpan, stock, addBtn);
            itemsLayout.add(row);
        }
        add(itemsLayout);

        H2 bidsHeader = new H2("📢 Bids for This Shop");
        add(bidsHeader);

        Grid<BidRecieptDTO> shopBidsGrid = new Grid<>(BidRecieptDTO.class, false);
        shopBidsGrid.addColumn(BidRecieptDTO::getPurchaseId)
                     .setHeader("Bid ID")
                     .setAutoWidth(true);
        shopBidsGrid.addColumn(dto -> dto.getInitialPrice())
                     .setHeader("Initial Price")
                     .setAutoWidth(true);
        shopBidsGrid.addColumn(dto -> dto.getHighestBid())
                     .setHeader("Highest Bid")
                     .setAutoWidth(true);
        shopBidsGrid.addColumn(dto -> dto.isCompleted() ? "Yes" : "No")
                     .setHeader("Completed");

        // When the user clicks a bid row, navigate to /bid/{purchaseId}
        shopBidsGrid.asSingleSelect().addValueChangeListener(event -> {
            BidRecieptDTO selected = event.getValue();
            if (selected != null) {
                UI.getCurrent().navigate("bid/" + selected.getPurchaseId());
            }
        });

        add(shopBidsGrid);

        fetchStoreBids(shopBidsGrid);

        // Reviews section
        add(new H2("📝 Reviews"));
        double avg = shop.getReviews().stream().mapToInt(ShopReviewDTO::getRating).average().orElse(0.0);
        add(new Paragraph("⭐ Average Rating: " + String.format("%.1f", avg) + "/5"));
        for (ShopReviewDTO rev : shop.getReviews()) {
            add(new Paragraph("👤 " + rev.getUserId() + ": " + rev.getReviewText() + " (" + rev.getRating() + ")"));
        }
    }
}
