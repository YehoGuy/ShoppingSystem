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
import com.vaadin.flow.component.notification.Notification;
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
 * Uses NumberField for quantity so the input is always clickable and visible.
 */
@Route(value = "shop/:shopId/create-bid", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class CreateBidView extends VerticalLayout implements BeforeEnterObserver {

    private String shopId;                          // path parameter from the URL
    private ShopDTO shop;                           // the loaded ShopDTO
    private Map<ItemDTO, Double> prices;            // map of item→price

    private final RestTemplate rest = new RestTemplate();
    
    @Value("${url.api}/shops")
    private String SHOP_API_URL;

    @Value("${url.api}/purchases/bids")
    private String CREATE_BID_URL;

    // For each item, we store a NumberField so we can read its numeric quantity later
    private final Map<ItemDTO, NumberField> qtyFields = new HashMap<>();

    // “Initial Price” at the bottom
    private final NumberField initialPriceField = new NumberField("Initial Price");

    // Button to post the bid
    private final Button createBidButton = new Button("Create Bid");

    public CreateBidView() {
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // 1) Extract the shopId path parameter
        shopId = event.getRouteParameters().get("shopId").orElse(null);
        if (shopId == null || shopId.trim().isEmpty()) {
            event.rerouteToError(NotFoundException.class);
            return;
        }
        // 2) Load the ShopDTO (with items & prices)
        loadShop();
        // 3) Build the UI once we have shop data
        buildPage();
    }

    private void loadShop() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null || token.isBlank()) {
            Notification.show("You must be logged in to view this page.", 
                              3000, Notification.Position.MIDDLE);
            shop = null;
            prices = null;
            return;
        }

        String url = SHOP_API_URL + "/" + shopId + "?token=" + token;
        try {
            ResponseEntity<ShopDTO> resp = rest.getForEntity(url, ShopDTO.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                shop = resp.getBody();
                // Build the item→price map (ShopDTO provides getItems() and getItemPrices())
                prices = ShopDTO.itemPricesToMapConverter(
                    shop.getItems(),    // List<ItemDTO>
                    shop.getItemPrices()// List<Double>, same length
                );
            } else {
                Notification.show("⚠️ Failed to load shop: " + resp.getStatusCode(), 
                                  3000, Notification.Position.MIDDLE);
                shop = null;
                prices = null;
            }
        } catch (Exception e) {
            Notification.show("❗ Error loading shop: " + e.getMessage(), 
                              3000, Notification.Position.MIDDLE);
            shop = null;
            prices = null;
        }
    }

    private void buildPage() {
        removeAll();           // clear any existing components
        qtyFields.clear();     // clear leftover references

        if (shop == null) {
            add(new H2("Unable to load shop."));
            return;
        }

        // 1. Header
        H2 header = new H2("Create New Bid for Shop: " + shop.getName());
        add(header);

        // 2. Fetch the list of items from the ShopDTO
        List<ItemDTO> items = (shop.getItems() == null) 
                             ? List.of() 
                             : shop.getItems();

        if (items.isEmpty()) {
            add(new H2("No items available for this shop. Cannot create a bid."));
            return;
        }

        // 3. For each item, create a row with name, price, and a NumberField for quantity
        for (ItemDTO item : items) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);

            // Item name
            com.vaadin.flow.component.html.Span nameSpan =
                new com.vaadin.flow.component.html.Span("🍽️ " + item.getName());
            // Item price (lookup in the prices map; default to 0.0)
            double p = (prices == null) 
                     ? 0.0 
                     : prices.getOrDefault(item, 0.0);
            com.vaadin.flow.component.html.Span priceSpan =
                new com.vaadin.flow.component.html.Span("💲 " + String.format("%.2f", p));

            // Quantity field (NumberField), so it’s obviously clickable
            NumberField qtyField = new NumberField();
            qtyField.setPlaceholder("0");         // shows “0” in gray when empty
            qtyField.setMin(0);
            qtyField.setStep(1);
            qtyField.setWidth("80px");            // fixed width so it’s visible
            qtyField.setValue(0.0);               // default 0

            // Store for later when the user clicks “Create Bid”
            qtyFields.put(item, qtyField);

            // Assemble the row
            row.add(nameSpan, priceSpan, qtyField);
            add(row);
        }

        // 4. “Initial Price” field below the item rows
        initialPriceField.setPlaceholder("Your Price");
        initialPriceField.setMin(1);
        initialPriceField.setStep(1);
        initialPriceField.setWidth("100px");
        initialPriceField.setValue(1.0);
        add(initialPriceField);

        // 5. “Create Bid” button
        createBidButton.addClickListener(e -> onCreateBidClick());
        add(createBidButton);
    }

    private void onCreateBidClick() {
        // 1) Check authToken
        String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (authToken == null || authToken.isBlank()) {
            Notification.show("You must be logged in to create a bid.", 
                              3000, Notification.Position.MIDDLE);
            return;
        }

        // 2) Check initial price
        Double initialPriceD = initialPriceField.getValue();
        int initialPrice = initialPriceD == null ? 0 : initialPriceD.intValue();
        if (initialPrice < 1) {
            Notification.show("Initial price must be at least 1.", 
                              3000, Notification.Position.MIDDLE);
            return;
        }

        // 3) Build itemsMap<itemId→quantity> for all qty > 0
        Map<Integer,Integer> itemsMap = qtyFields.entrySet().stream()
            .filter(en -> {
                Double v = en.getValue().getValue();
                return v != null && v.intValue() > 0;
            })
            .collect(Collectors.toMap(
                en -> en.getKey().getId(),
                en -> en.getValue().getValue().intValue()
            ));

        if (itemsMap.isEmpty()) {
            Notification.show("Select at least one item and set quantity > 0.", 
                              3000, Notification.Position.MIDDLE);
            return;
        }

        // 4) Build the POST URL
        String url = CREATE_BID_URL
                   + "?authToken="    + authToken
                   + "&storeId="      + shopId     // string form of shopId
                   + "&initialPrice=" + initialPrice;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<Integer,Integer>> entity = new HttpEntity<>(itemsMap, headers);

        try {
            ResponseEntity<Integer> resp = rest.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
            );

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                int newBidId = resp.getBody();
                Notification.show("Bid created with ID " + newBidId, 
                                  2000, Notification.Position.MIDDLE);
                UI.getCurrent().navigate("bid/" + newBidId);
            } else {
                Notification.show("Failed to create bid: " + resp.getStatusCode(), 
                                  3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("❗ Error creating bid: " + ex.getMessage(), 
                              4000, Notification.Position.MIDDLE);
        }
    }
}
