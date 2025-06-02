package UI;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.server.VaadinSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import DTOs.ShopDTO;
import DTOs.ItemDTO;
import DTOs.ShopReviewDTO;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

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
        handleSuspence();
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
        for (Map.Entry<ItemDTO, Integer> entry : ShopDTO
                .itemQuantitiesToMapConverter(shop.getItems(), shop.getItemQuantities()).entrySet()) {
            ItemDTO item = entry.getKey();
            int available = entry.getValue();
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);

            Span name = new Span("🍽️ " + item.getName());
            Span priceSpan = new Span("💲 " + prices.getOrDefault(item, 0.0));
            Span stock = new Span("📊 In Stock: " + available);
            Button addBtn = new Button("🛒 Add to Cart", evt -> Notification.show("🚀 Added " + item.getName()));
            if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
                addBtn.setVisible(false);
            }
            row.add(name, priceSpan, stock, addBtn);
            itemsLayout.add(row);
        }
        add(itemsLayout);

        // Reviews section
        add(new H2("📝 Reviews"));
        double avg = shop.getReviews().stream().mapToInt(ShopReviewDTO::getRating).average().orElse(0.0);
        add(new Paragraph("⭐ Average Rating: " + String.format("%.1f", avg) + "/5"));
        for (ShopReviewDTO rev : shop.getReviews()) {
            add(new Paragraph("👤 " + rev.getUserId() + ": " + rev.getReviewText() + " (" + rev.getRating() + ")"));
        }
    }
    
    private void handleSuspence() {
        Integer userId = (Integer) VaadinSession.getCurrent().getAttribute("userId");
        if (userId == null) {
            return;
        }
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            return;
        }
        String url = "http://localhost:8080/api/users" + "/"+userId+"/suspension?token=" +token;
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            throw new RuntimeException(
                "Failed to check admin status: HTTP " + response.getStatusCode().value()
            );
        }
    }
}
