package UI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import DTOs.ItemDTO;
import DTOs.ShopDTO;

/**
 * Route: /shop/{shopId}/create-bid
 * Displays each item in its own HorizontalLayout row (name, price, quantity field).
 * Enforces that the initial total bid price ≥ sum(itemPrice × quantity).
 * If the store owner tries to bid, show a notification and redirect to "bids".
 */
@Route(value = "shop/:shopId/create-bid/:itemId", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class CreateBidView extends VerticalLayout implements BeforeEnterObserver {

    private String shopId;
    private String itemId;
    private ShopDTO shop;
    private Map<ItemDTO, Double> prices;

    private final RestTemplate rest = new RestTemplate();
    private final String shopApiBase;
    private final String createBidUrl;
    private final String userApiBase;

    // quantity fields per item
    private final Map<ItemDTO, NumberField> qtyFields = new HashMap<>();

    // initial total price
    private final NumberField initialPriceField = new NumberField("Initial Price");

    // submit button
    private final Button createBidButton = new Button("Create Bid");

    public CreateBidView(@Value("${url.api}") String apiBase) {
        this.shopApiBase  = apiBase + "/shops";
        this.createBidUrl = apiBase + "/purchases/bids";
        this.userApiBase  = apiBase + "/users";

        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // ensure logged in
        if (getUserId() == null) {
            return;
        }

        // extract shopId
        shopId = event.getRouteParameters().get("shopId").orElse(null);
        if (shopId == null || shopId.isBlank()) {
            event.rerouteToError(NotFoundException.class);
            return;
        }

        itemId = event.getRouteParameters().get("itemId").orElseThrow(NotFoundException::new);


        loadShop();
        // redirect owner to bids
        if (isOwner()) {
            //TODO: wait a sec so the user could read the message
            Notification.show("❌ You cannot place a bid on your own shop.", 3000, Position.MIDDLE);
            event.rerouteTo("bids");
            return;
        }

        buildPage();
    }

    private Integer getUserId() {
        Object uid = VaadinSession.getCurrent().getAttribute("userId");
        if (uid == null) {
            UI.getCurrent().navigate("");
            return null;
        }
        return Integer.valueOf(uid.toString());
    }

    private void loadShop() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("You must be logged in to view this page.", 3000, Position.MIDDLE);
            shop = null;
            prices = null;
            return;
        }
        String url = shopApiBase + "/" + shopId + "?token=" + token;
        try {
            ResponseEntity<ShopDTO> resp = rest.getForEntity(url, ShopDTO.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                shop = resp.getBody();
                prices = ShopDTO.itemPricesToMapConverter(shop.getItems(), shop.getItemPrices());
            } else {
                shop = null;
                prices = null;
                Notification.show("⚠️ Failed to load shop", 3000, Position.MIDDLE);
            }
        } catch (Exception e) {
            shop = null;
            prices = null;
            Notification.show("❗ Error loading shop", 3000, Position.MIDDLE);
        }
    }

    private boolean isOwner() {
        Integer me = getUserId();
        if (shop == null || me == null) return false;
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String ownerUrl = userApiBase + "/shops/" + shopId + "/owner?token=" + token;
        try {
            ResponseEntity<Integer> resp = rest.getForEntity(ownerUrl, Integer.class);
            return resp.getStatusCode().is2xxSuccessful() && me.equals(resp.getBody());
        } catch (Exception e) {
            return false;
        }
    }

    private void buildPage() {
        removeAll();
        qtyFields.clear();
        if (shop == null) {
            add(new H2("Unable to load shop."));
            return;
        }
        add(new H2("Create New Bid for Shop: " + shop.getName()));

        List<ItemDTO> allItems = shop.getItems() == null ? List.of() : shop.getItems();
        List<ItemDTO> items =
                                allItems.stream()
                                        .filter(i -> String.valueOf(i.getId()).equals(itemId))
                                        .collect(Collectors.toList());
        
        if (items.isEmpty()) {
            add(new H2("No items available for this shop. Cannot create a bid."));
            return;
        }

        // items + qty fields
        for (ItemDTO item : items) {
            double price = prices.getOrDefault(item, 0.0);
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);
            Span nameSpan = new Span("The item: " + item.getName());
            Span priceSpan = new Span("💲 " + String.format("%.2f", price));
            NumberField qtyField = new NumberField();
            qtyField.setPlaceholder("set Qty");
            qtyField.setMin(1);
            qtyField.setStep(1);
            qtyField.setWidth("100px");
            // qtyField.setValue(1.0);
            qtyFields.put(item, qtyField);
            row.add(nameSpan, priceSpan, qtyField);
            add(row);
        }

        initialPriceField.setPlaceholder("Your bid price");
        initialPriceField.setMin(0.0);
        initialPriceField.setStep(1);
        initialPriceField.setValue(0.0);
        add(initialPriceField);

        createBidButton.addClickListener(e -> onCreateBidClick());
        add(createBidButton);
    }

    private void onCreateBidClick() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("You must be logged in to create a bid.", 3000, Position.MIDDLE);
            return;
        }
        Map<Integer,Integer> itemsMap = qtyFields.entrySet().stream()
            .filter(en -> en.getValue().getValue() != null && en.getValue().getValue() > 0)
            .collect(Collectors.toMap(
                en -> en.getKey().getId(),
                en -> en.getValue().getValue().intValue()
            ));
        if (itemsMap.isEmpty()) {
            Notification.show("Select at least one item with quantity > 0.", 3000, Position.MIDDLE);
            return;
        }
        Double entered = initialPriceField.getValue();
        if (entered == null || entered <= 0) {
            Notification.show(
                "Please enter a bid price greater > 0.",
                3000, Position.MIDDLE);
            return;
        }
        String url = createBidUrl + "?authToken=" + token
                   + "&storeId=" + shopId
                   + "&initialPrice=" + entered.intValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<Integer,Integer>> entity = new HttpEntity<>(itemsMap, headers);
        try {
            ResponseEntity<Integer> resp = rest.exchange(
                url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {});
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Notification.show("Bid created with ID " + resp.getBody(), 2000, Position.MIDDLE);
                UI.getCurrent().navigate("bids");
            } else {
                Notification.show("Failed to create bid", 3000, Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("Error creating bid", 3000, Position.MIDDLE);
        }
    }
}
