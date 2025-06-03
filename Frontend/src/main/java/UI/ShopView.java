package UI;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import DTOs.BidRecieptDTO;                  // for Map.Entry
import DTOs.ItemDTO;                 // if you use List elsewhere
import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;

@Route(value = "shop", layout = AppLayoutBasic.class)
@JsModule("./js/notification-client.js")
public class ShopView extends VerticalLayout implements HasUrlParameter<String>, BeforeEnterObserver {

    private static final String SHOP_API_URL = "http://localhost:8080/api/shops";
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
            String url = "http://localhost:8080/api/purchases/shops/"
                    + shop.getShopId()
                    + "/bids?authToken="
                    + authToken;

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

        for (Map.Entry<ItemDTO, Integer> e :
                ShopDTO.itemQuantitiesToMapConverter(
                        shop.getItems(),
                        shop.getItemQuantities()
                ).entrySet()) {

            ItemDTO item = e.getKey();
            int available = e.getValue();

            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);
            row.getStyle().set("align-items", "center");

            Span name = new Span("🍽️ " + item.getName());
            Span priceSpan = new Span("💲 " + prices.getOrDefault(item, 0.0));
            Span stock = new Span("📊 In Stock: " + available);

            // IntegerField to choose quantity
            IntegerField qtyField = new IntegerField();
            qtyField.setLabel("Quantity");
            qtyField.setValue(1);
            qtyField.setMin(1);
            qtyField.setMax(available);
            qtyField.setStepButtonsVisible(true);
            qtyField.setWidth("80px");

            // “Add to Cart” button with chosen quantity
            Button addBtn = new Button("🛒 Add to Cart", evt -> {
                // Read chosen quantity (default to 1 if null or invalid)
                Integer chosenQty = qtyField.getValue();
                int qty = (chosenQty != null && chosenQty > 0) ? chosenQty : 1;

                // Ensure it does not exceed available stock
                if (qty > available) {
                    Notification.show("❌ Only " + available + " in stock");
                    return;
                }

                // Check for auth token in VaadinSession
                String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
                if (authToken == null || authToken.isBlank()) {
                    Notification.show("❌ Please log in first");
                    return;
                }

                // Build URL: POST http://localhost:8080/shops/{shopId}/cart/add?itemId={itemId}&quantity={qty}&token={authToken}
                String url = "http://localhost:8080/api/users/"
                        + "/shoppingCart/"
                        + shop.getShopId()
                        + "/"
                        + item.getId()
                        + "?quantity=" + qty
                        + "&token=" + authToken;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Void> request = new HttpEntity<>(headers);

                try {
                    ResponseEntity<Void> resp = restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            request,
                            Void.class
                    );

                    if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
                        Notification.show("🚀 Added “" + item.getName() + "” x" + qty + " to cart");
                    } else {
                        Notification.show("❌ Could not add to cart: " + resp.getStatusCode());
                    }
                } catch (Exception ex) {
                    Notification.show("❌ Error adding to cart: " + ex.getMessage());
                }
            });

            row.add(name, priceSpan, stock, qtyField, addBtn);
            itemsLayout.add(row);
        }

        add(itemsLayout);

        // Bids section
        H2 bidsHeader = new H2("📢 Bids for This Shop");
        add(bidsHeader);
        Grid<BidRecieptDTO> shopBidsGrid = new Grid<>(BidRecieptDTO.class, false);
        shopBidsGrid.addColumn(BidRecieptDTO::getPurchaseId)
                    .setHeader("Bid ID")
                    .setAutoWidth(true);
        shopBidsGrid.addColumn(dto -> dto.getInitialPrice())
                    .setHeader("Initial Price")
                    .setAutoWidth(true);
        fetchStoreBids(shopBidsGrid);
        add(shopBidsGrid);

        // Reviews section
        add(new H2("📝 Reviews"));
        double avg = shop.getReviews().stream()
                        .mapToInt(ShopReviewDTO::getRating)
                        .average()
                        .orElse(0.0);
        add(new Paragraph("⭐ Average Rating: " + String.format("%.1f", avg) + "/5"));
        for (ShopReviewDTO rev : shop.getReviews()) {
            add(new Paragraph("👤 " + rev.getUserId() + ": "
                            + rev.getReviewText() + " (" + rev.getRating() + ")"));
        }
    }
}
