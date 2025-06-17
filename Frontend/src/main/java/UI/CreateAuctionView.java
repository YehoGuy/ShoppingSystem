package UI;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
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
 * View for managers to create a timed auction.
 */
@Route(value = "shop/:shopId/create-auction", layout = AppLayoutBasic.class)
@AnonymousAllowed
public class CreateAuctionView extends VerticalLayout implements BeforeEnterObserver {
    private String shopId; // path parameter from the URL
    private ShopDTO shop; // the loaded ShopDTO
    private Map<ItemDTO, Double> prices; // map of item→price

    private final RestTemplate rest = new RestTemplate();

    private final String shopApiBase;
    
    private final String createAuctionUrl;

    // For each item, we store a NumberField so we can read its numeric quantity
    // later
    private final Map<ItemDTO, NumberField> qtyFields = new HashMap<>();

    // “Initial Price” at the bottom
    private final NumberField initialPriceField = new NumberField("Initial Price");

    // DateTimePicker for auction end time
    private final DateTimePicker endTimePicker = new DateTimePicker("Auction end time");

    // Button to post the auction
    private final Button createAuctionButton = new Button("Create Auction");

    public CreateAuctionView(@Value("${url.api}") String apiBase) {
        this.shopApiBase      = apiBase + "/shops";
        this.createAuctionUrl = apiBase + "/purchases/auctions";

        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // Ensure userId is set in session
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

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
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

        String url = shopApiBase + "/" + shopId + "?token=" + token;
        try {
            ResponseEntity<ShopDTO> resp = rest.getForEntity(url, ShopDTO.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                shop = resp.getBody();
                // Build the item→price map (ShopDTO provides getItems() and getItemPrices())
                prices = ShopDTO.itemPricesToMapConverter(
                        shop.getItems(), // List<ItemDTO>
                        shop.getItemPrices()// List<Double>, same length
                );
            } else {
                Notification.show("⚠️ Failed to load shop",
                        3000, Notification.Position.MIDDLE);
                shop = null;
                prices = null;
            }
        } catch (Exception e) {
            Notification.show("❗ Error loading shop",
                    3000, Notification.Position.MIDDLE);
            shop = null;
            prices = null;
        }
    }

    private void buildPage() {
    removeAll();

    if (shop == null) {
        add(new H2("Unable to load shop."));
        return;
    }

    // 1. Header
    H2 header = new H2("Create New Auction for Shop: " + shop.getName());
    add(header);

    // 2. Build a ComboBox so the manager picks exactly one item
    List<ItemDTO> items = shop.getItems() == null ? List.of() : shop.getItems();
    ComboBox<ItemDTO> itemSelect = new ComboBox<>("Select Item");
    itemSelect.setItems(items);
    itemSelect.setItemLabelGenerator(ItemDTO::getName);
    itemSelect.setPlaceholder("Choose one item");
    itemSelect.setWidth("300px");
    add(itemSelect);

    // 3. Show fixed quantity = 1
    Span qtySpan = new Span("Quantity: 1");
    add(qtySpan);

    // 4. Initial Price field, disabled until item is chosen
    initialPriceField.setWidth("120px");
    initialPriceField.setEnabled(false);
    add(initialPriceField);

    // 5. End-time picker
    endTimePicker.setMin(LocalDateTime.now().plusMinutes(1));
    endTimePicker.setStep(Duration.ofMinutes(1));
    endTimePicker.setValue(LocalDateTime.now().plusHours(1));
    add(endTimePicker);

    // 6. Create button
    createAuctionButton.addClickListener(e -> onCreateAuctionClick(itemSelect));
    add(createAuctionButton);

    // 7. When an item is picked, initialize & unlock the price field
    itemSelect.addValueChangeListener(ev -> {
        ItemDTO chosen = ev.getValue();
        if (chosen != null) {
            double p = prices.getOrDefault(chosen, 0.0);
            initialPriceField.setMin(p);
            initialPriceField.setValue(p);
            initialPriceField.setEnabled(true);
        } else {
            initialPriceField.clear();
            initialPriceField.setEnabled(false);
        }
    });
}

private void onCreateAuctionClick(ComboBox<ItemDTO> itemSelect) {
    String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
    if (token == null || token.isBlank()) {
        Notification.show("Login required", 3000, Notification.Position.MIDDLE);
        return;
    }

    // 1) Which item?
    ItemDTO chosen = itemSelect.getValue();
    if (chosen == null) {
        Notification.show("Please select an item", 3000, Notification.Position.MIDDLE);
        return;
    }

    // 2) Quantity is always 1
    Map<Integer,Integer> itemsMap = Map.of(chosen.getId(), 1);

    // 3) Initial price ≥ item price?
    Double ip = initialPriceField.getValue();
    double itemPrice = prices.getOrDefault(chosen, 0.0);
    if (ip == null || ip < itemPrice) {
        Notification.show(
          "Initial price must be at least the item’s base price (" + itemPrice + ")",
          3000, Notification.Position.MIDDLE);
        return;
    }
    int initialPrice = ip.intValue();

    // 4) End time validation
    LocalDateTime end = endTimePicker.getValue();
    if (end == null || end.isBefore(LocalDateTime.now().plusSeconds(30))) {
        Notification.show("End time must be ≥ 30 seconds from now", 3000, Notification.Position.MIDDLE);
        return;
    }

    // 5) Build URL & POST body
    String url = createAuctionUrl
        + "?authToken="      + token
        + "&storeId="        + shopId
        + "&initialPrice="   + initialPrice
        + "&auctionEndTime=" + end;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<Integer,Integer>> entity = new HttpEntity<>(itemsMap, headers);

    try {
        ResponseEntity<Integer> resp = rest.exchange(
            url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {});
        if (resp.getStatusCode().is2xxSuccessful()) {
            Notification.show("Auction created!", 2000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("auctions");
        } else {
            Notification.show("Failed to create auction", 3000, Notification.Position.MIDDLE);
        }
    } catch (Exception ex) {
        Notification.show("Error creating auction", 3000, Notification.Position.MIDDLE);
    }
}

}

