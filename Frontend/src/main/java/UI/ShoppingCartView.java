package UI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;

import DTOs.BidRecieptDTO;
import DTOs.CartEntryDTO;
import DTOs.ItemDTO;
import DTOs.ShopDTO;
import DTOs.ShoppingCartDTO;

@Route(value = "cart", layout = AppLayoutBasic.class)

public class ShoppingCartView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger log = LoggerFactory.getLogger(ShoppingCartView.class);
    private ShoppingCartDTO cart;
    private List<ShopDTO> shops;

    private final RestTemplate restTemplate = new RestTemplate();

    private String URLShop;
    private String URLUser;
    private String URLPurchases;
    private String URLItem;
    private String baseUrl;

    private double totalPrice = 0.0;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // Ensure userId is set in session
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
            return;
        }

        handleSuspence();

        if(!isGuest())
        {
            getWonAuctionsSection();
            getFinishedBidsSection();
        }

    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    public ShoppingCartView(@Value("${url.api}") String baseUrl) {
        this.URLShop = baseUrl + "/shops";
        this.URLUser = baseUrl + "/users";
        this.URLPurchases = baseUrl + "/purchases";
        this.URLItem = baseUrl + "/items";
        this.baseUrl = baseUrl;

        
        setSizeFull();
        setSpacing(true);
        setPadding(true);
        setAlignItems(Alignment.CENTER);
        buildView();
    }

    private void buildView() {
        getData();

        this.totalPrice = getTotalAllShops();
        
        if (cart.getShopItems() == null || cart.getShopItems().isEmpty()) {
            H2 empty = new H2("Your shopping cart is empty 😕");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(empty);

            return;
        }

        H1 title = new H1("Shopping cart");

        Div buyButtonContainer = new Div();
        buyButtonContainer.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("right", "0")
                .set("margin", "80px")
                .set("padding", "10px");

        Button buyButton = new Button("Buy entire cart");
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            buyButton.setVisible(false);
        }
        buyButton.addClickListener(event -> {
            try {

                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Purchase Summary");

                PurchaseCompletionIntermidiate purchaseCompletion = new PurchaseCompletionIntermidiate(baseUrl, cart,
                        dialog, -1, totalPrice );/////////////

                // Add your component to the dialog
                dialog.add(purchaseCompletion);

                // Optional: add a close button in the footer
                Button closeButton = new Button("Close", e -> dialog.close());
                dialog.getFooter().add(closeButton);

                dialog.open(); // Show the dialog
                buildView(); // Refresh the view after purchase
            } catch (Exception e) {
                Notification.show("Failed to proceed with purchase. Please try again later.",
                        3000, Notification.Position.MIDDLE);
                log.warn("Could not proceed with purchase", e);
            }
        });
        buyButton.getStyle().set("background-color", "red").set("color", "white");
        buyButtonContainer.add(buyButton);

        add(title);

        // For each shop in the cart
        cart.getShopItems().forEach((shopID, itemIDs) -> {
            String shopName = getShopName(shopID);
            Map<Integer, Double> itemPricesMap = cart.getShopItemPrices().get(shopID);
            Map<Integer, Integer> itemQuantitiesMap = cart.getShopItemQuantities().get(shopID);

            double shopTotal = getShopPrice(shopID);
            List<CartEntryDTO> entries = new ArrayList<>();
            for (Integer itemId : itemIDs) {
                int quantity = itemQuantitiesMap != null && itemQuantitiesMap.containsKey(itemId)
                        ? itemQuantitiesMap.get(itemId)
                        : 1;
                ItemDTO item = getAllItems().stream().filter(it -> it.getId() == itemId).findFirst().orElse(null);
                if (item != null) {
                    entries.add(new CartEntryDTO(quantity, item, shopID));
                }
            }

            Button buyBasketButton = new Button("Buy basket from " + shopName + " price after discounts: " + shopTotal + "₪");
            if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
                buyBasketButton.setVisible(false);
            }
            buyBasketButton.getStyle().set("background-color", "blue").set("color", "white");
            buyBasketButton.addClickListener(event -> {

                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Purchase Summary");
                ShoppingCartDTO newCart = cart.getShoppingCartDTOofShop(shopID);
                PurchaseCompletionIntermidiate purchaseCompletion = new PurchaseCompletionIntermidiate(
                        baseUrl, cart.getShoppingCartDTOofShop(shopID), dialog, shopID, shopTotal);

                // Add your component to the dialog
                dialog.add(purchaseCompletion);

                // Optional: add a close button in the footer
                Button closeButton = new Button("Close", e -> dialog.close());
                dialog.getFooter().add(closeButton);

                dialog.open(); // Show the dialog
                buildView(); // Refresh the view after purchase
            });
            H3 shopHeader = new H3(shopName + " - total price after discounts: " + shopTotal + "₪");
            VerticalLayout shopHeaderContainer = new VerticalLayout(shopHeader, buyBasketButton);
            shopHeaderContainer.setWidthFull();
            shopHeaderContainer.setAlignItems(Alignment.CENTER);
            shopHeaderContainer.setJustifyContentMode(JustifyContentMode.BETWEEN);
            add(shopHeaderContainer);

            Grid<ItemRow> grid = new Grid<>(ItemRow.class, false);
            grid.addColumn(ItemRow::name).setHeader("Item name");
            grid.addColumn(ItemRow::price).setHeader("Price");
            grid.addColumn(ItemRow::quantity).setHeader("Quantity");
            grid.addColumn(ItemRow::totalPrice).setHeader("Total price");
            grid.addColumn(ItemRow::description).setHeader("Description");
            grid.setAllRowsVisible(true);
            grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

            List<ItemRow> rows = new ArrayList<>();
            for (CartEntryDTO entry : entries) {
                ItemDTO item = entry.getItem();
                int quantity = entry.getQuantity();
                Double price = itemPricesMap != null && itemPricesMap.containsKey(item.getId())
                        ? itemPricesMap.get(item.getId())
                        : 0;
                rows.add(new ItemRow(item, quantity, price, item.getId()));
            }

            grid.setItems(rows);
            grid.addComponentColumn(renderer -> {
                Button removeButton = new Button(VaadinIcon.MINUS.create());
                Button addButton = new Button(VaadinIcon.PLUS.create());
                Button removeCompletlyButton = new Button(VaadinIcon.TRASH.create());
                if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
                    removeButton.setVisible(false);
                    addButton.setVisible(false);
                    removeCompletlyButton.setVisible(false);

                }

                removeCompletlyButton.addClickListener(event -> {

                    handleCartAction(shopID, renderer.itemId(), "remove");
                });
                addButton.addClickListener(event -> {
                    handleCartAction(shopID, renderer.itemId(), "plus");
                });
                removeButton.addClickListener(event -> {
                    handleCartAction(shopID, renderer.itemId(), "minus");

                });
                HorizontalLayout buttonLayout = new HorizontalLayout(addButton, removeButton, removeCompletlyButton);
                buttonLayout.setAlignItems(Alignment.CENTER);
                buttonLayout.setSpacing(true);
                return buttonLayout;
            }).setHeader("Auctions").setFlexGrow(0).setWidth("200px");
            add(grid);
        });

        


        HorizontalLayout totalContainer = new HorizontalLayout();
        totalContainer.setAlignItems(Alignment.CENTER);
        totalContainer.getStyle()
                .set("bottom", "0")
                .set("right", "0")
                .set("margin", "0px");
        
        H2 total = new H2("Total after discounts: " + totalPrice  + "₪");
        totalContainer.add(total, buyButton);
        add(buyButtonContainer, totalContainer);


        if(!isGuest())
        {

            // Your Won Auctions section
            H2 wonHeader = new H2("Your Won Auctions");
            add(wonHeader);

            // Fetch the list of won auctions for this user
            String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
            HttpHeaders wonHeaders = new HttpHeaders();
            wonHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> wonEntity = new HttpEntity<>(wonHeaders);
            ResponseEntity<List<BidRecieptDTO>> wonResp = restTemplate.exchange(
                    URLPurchases + "/auctions/won?authToken=" + token,
                    HttpMethod.GET,
                    wonEntity,
                    new ParameterizedTypeReference<List<BidRecieptDTO>>() {
                    },
                    token);
            List<BidRecieptDTO> wonList = wonResp.getBody();

            // Build and display the grid
            Grid<BidRecieptDTO> wonGrid = new Grid<>(BidRecieptDTO.class, false);
            wonGrid.addColumn(BidRecieptDTO::getPurchaseId)
                    .setHeader("Auction ID")
                    .setAutoWidth(true);
            wonGrid.addColumn(BidRecieptDTO::getHighestBid)
                    .setHeader("Winning Bid")
                    .setAutoWidth(true);
            wonGrid.addComponentColumn(dto -> {
                Button payNow = new Button("Pay Now");
                payNow.addClickListener(e -> {

                    payForBid(dto);

                });
                return payNow;
            })
                    .setHeader("Auction")
                    .setAutoWidth(true);

            if (wonList != null) {
                wonGrid.setItems(wonList);
            }
            add(wonGrid);
        }

    }

    private Double getTotalAllShops() {
        
        List<Integer> shopIds = new ArrayList<>(cart.getShopItems().keySet());
        Double total = 0.0;
        for (Integer shopId : shopIds) {
            total += getShopPrice(shopId);
        }
        return total;
    }

    private Double getShopPrice(int shopId) {
        String token = VaadinSession.getCurrent().getAttribute("authToken").toString();
        Map<Integer, Integer> itemQuantities = cart.getShopItemQuantities().get(shopId); 

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<Integer, Integer>> request = new HttpEntity<>(itemQuantities, headers);

        String url = URLShop + "/applyDiscount/cart?shopId=" + shopId + "&token=" + token;

        try {
            ResponseEntity<Double> response = restTemplate.postForEntity(url, request, Double.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            Notification.show("Failed to calculate shop price", 3000, Notification.Position.MIDDLE);
            log.warn("Discount calculation failed for shop {}", shopId, e);
        }

        return 0.0;
}


    private void getData() {
        HashMap<Integer, HashMap<Integer, Integer>> IDs = getCartIDs();
        // Initialize cart with empty collections first
        cart = new ShoppingCartDTO();
        cart.setShopItems(new HashMap<>());
        cart.setShopItemPrices(new HashMap<>());
        cart.setShopItemQuantities(new HashMap<>());

        if (IDs.isEmpty()) {
            shops = new ArrayList<>();
            return; // Return early if cart is empty
        }

        shops = getShopNames(IDs.keySet());
        List<ItemDTO> items = getAllItems();

        // Build DTO fields
        Map<Integer, List<Integer>> shopItems = new HashMap<>();
        Map<Integer, Map<Integer, Double>> shopItemPrices = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> shopItemQuantities = new HashMap<>();

        for (ShopDTO shop : shops) {
            int shopId = shop.getShopId();
            HashMap<Integer, Integer> itemQuantities = IDs.get(shopId);
            if (itemQuantities == null)
                continue;
            List<Integer> itemIds = new ArrayList<>(itemQuantities.keySet());
            shopItems.put(shopId, itemIds);

            Map<Integer, Double> prices = new HashMap<>();
            for (Integer itemId : itemIds) {
                ItemDTO item = items.stream().filter(it -> it.getId() == itemId).findFirst().orElse(null);
                double price = 0;
                if (item != null && shop.getItemPrices() != null && shop.getItemPrices().containsKey(itemId)) {
                    price = shop.getItemPrices().get(itemId).intValue();
                }
                prices.put(itemId, price);
            }
            shopItemPrices.put(shopId, prices);
            shopItemQuantities.put(shopId, itemQuantities);
        }

        cart.setShopItems(shopItems);
        cart.setShopItemPrices(shopItemPrices);
        cart.setShopItemQuantities(shopItemQuantities);
        cart.setItems(items);
    }

    private List<ItemDTO> getAllItems() {
        try {
            String token = VaadinSession.getCurrent().getAttribute("authToken").toString();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<List<ItemDTO>> response = restTemplate.exchange(
                    URLItem + "/all?token=" + token,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<ItemDTO>>() {
                    },
                    token);
            List<ItemDTO> items = response.getBody();
            return (items != null) ? items : Collections.emptyList();
        } catch (HttpClientErrorException.NotFound nf) {
            // no items ⇒ empty
            log.warn("No items found in the database, returning empty list");
            return Collections.emptyList();
        } catch (Exception e) {
            // swallow silently
            log.warn("Failed to fetch item list—returning empty", e);
            return Collections.emptyList();
        }
    }

    private List<ShopDTO> getShopNames(Set<Integer> shopIds) {
        List<ShopDTO> result = new ArrayList<>();
        String token = VaadinSession.getCurrent().getAttribute("authToken").toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (Integer id : shopIds) {
            try {
                ResponseEntity<ShopDTO> resp = restTemplate.exchange(
                        URLShop + "/" + id + "?token=" + token,
                        HttpMethod.GET,
                        entity,
                        ShopDTO.class);
                if (resp.getBody() != null) {
                    result.add(resp.getBody());
                }
            } catch (HttpClientErrorException.NotFound nf) {
                // skip
                log.warn("Shop with ID {} not found, skipping", id);
            } catch (Exception e) {
                // skip all other errors silently
                log.warn("Error fetching shop with ID {}", id);
                return Collections.emptyList();
            }
        }
        return result;
    }

    private HashMap<Integer, HashMap<Integer, Integer>> getCartIDs() {
        try {
            String token = VaadinSession.getCurrent().getAttribute("authToken").toString();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<HashMap<Integer, HashMap<Integer, Integer>>> resp = restTemplate.exchange(

                    URLUser + "/shoppingCart?token=" + token + "&userId=" + getUserId(),

                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    },
                    token);
            HashMap<Integer, HashMap<Integer, Integer>> body = resp.getBody();
            if (body == null || body.isEmpty()) {
                Dialog dialog = new Dialog();
                dialog.add(new H2("No items in your cart!"));
                dialog.add(new Button("OK", e -> dialog.close()));
                dialog.open();
            }
            return (body != null) ? body : new HashMap<>();
        } catch (HttpClientErrorException.NotFound nf) {
            // no cart yet ⇒ empty
            return new HashMap<>();
        } catch (Exception e) {
            // swallow everything else silently
            return new HashMap<>();
        }
    }

    // Helper record class to populate the grid
    public record ItemRow(String name, double price, int quantity, double totalPrice, String description, int itemId) {
        public ItemRow(ItemDTO item, int quantity, double price, int itemId) {
            this(item.getName(), price, quantity, price * quantity, item.getDescription(), itemId);
        }
    }

    private String getShopName(int shopId) {
        return shops.stream()
                .filter(shop -> shop.getShopId() == shopId)
                .map(ShopDTO::getName)
                .findFirst()
                .orElse("Unknown Shop");
    }

    private void handleCartAction(int shopID, int itemId, String action) {

        try {
            String token = VaadinSession.getCurrent().getAttribute("authToken").toString();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            if (itemId < 0)
                return;

            restTemplate.postForEntity(

                    URLUser + "/shoppingCart/" + shopID + "/" + itemId + "/" + action + "?token=" + token + "&userId="
                            + getUserId(),

                    entity,
                    Void.class,
                    token);
            resetView();
        } catch (Exception e) {
            // completely silent on failure
            Notification.show("Failed to update shopping cart. Please try again later.",
                    3000, Notification.Position.MIDDLE);
            log.warn("Could not retrieve shopping cart, treating as empty", e);
        }
    }

    private void resetView() {
        this.removeAll();
        this.cart = null;
        this.shops = null;
        buildView();
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
        String url = URLUser + "/" + userId + "/isSuspended?token=" + token;
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }

    private void getWonAuctionsSection() {
        add(new H2("Auctions You Won"));

        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");

        List<BidRecieptDTO> won;
        try {
            ResponseEntity<List<BidRecieptDTO>> resp = restTemplate.exchange(
                    URLPurchases + "/auctions/won?authToken=" + token,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    new ParameterizedTypeReference<List<BidRecieptDTO>>() {
                    },
                    token);
            won = resp.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Could be 404, 500, etc.
            H3 error = new H3("Could not load your won auctions right now.");
            error.getStyle().set("color", "var(--lumo-error-text-color)");
            add(error);
            return;
        }

        if (won == null || won.isEmpty()) {
            H3 empty = new H3("You haven't won any auctions yet.");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(empty);
            return;
        }

        Grid<BidRecieptDTO> grid = new Grid<>(BidRecieptDTO.class, false);
        // 1) Store Name
        grid.addColumn(dto -> fetchShopName(dto.getStoreId()))
                .setHeader("Store Name")
                .setAutoWidth(true);

        // 2) Item Name
        grid.addColumn(dto -> fetchItemName(dto.getStoreId(), dto))
                .setHeader("Item Name")
                .setAutoWidth(true);

        // 3) Your Winning Bid
        grid.addColumn(BidRecieptDTO::getHighestBid)
                .setHeader("Your Winning Bid")
                .setAutoWidth(true);

        // 4) “Pay Now”
        grid.addComponentColumn(dto -> {
            Button payNow = new Button("Pay Now");
            payNow.addClickListener(e -> payForBid(dto));
            return payNow;
        })
                .setHeader("For Payment")
                .setAutoWidth(true);

        grid.setItems(won);
        add(grid);
    }

    private void getFinishedBidsSection() {
        add(new H2("Finished Bids"));

        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<BidRecieptDTO> finished;
        try {
            ResponseEntity<List<BidRecieptDTO>> resp = restTemplate.exchange(
                    URLPurchases + "/bids/finished?authToken=" + token,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<BidRecieptDTO>>() {
                    },
                    token);
            finished = resp.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // log if you like: LOG.error("Failed to load finished bids", e);
            H3 error = new H3("Could not load your finished bids right now.");
            error.getStyle().set("color", "var(--lumo-error-text-color)");
            add(error);
            return;
        }

        if (finished == null || finished.isEmpty()) {
            H3 empty = new H3("You have no finished bids.");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(empty);
            return;
        }

        Grid<BidRecieptDTO> grid = new Grid<>(BidRecieptDTO.class, false);

        // 1) Store Name
        grid.addColumn(dto -> fetchShopName(dto.getStoreId()))
                .setHeader("Store Name")
                .setAutoWidth(true);

        // 2) Item Name
        grid.addColumn(dto -> fetchItemName(dto.getStoreId(), dto))
                .setHeader("Item Name")
                .setAutoWidth(true);

        // 3) Your bid amount
        grid.addColumn(BidRecieptDTO::getHighestBid)
                .setHeader("Your Bid")
                .setAutoWidth(true);

        // 4) “Pay Now”
        grid.addComponentColumn(dto -> {
            Button payNow = new Button("Pay Now");
            payNow.addClickListener(e -> payForBid(dto));
            return payNow;
        })
                .setHeader("For Payment")
                .setAutoWidth(true);

        grid.setItems(finished);
        add(grid);
    }

    private String fetchShopName(int shopId) {
        try {
            String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
            ResponseEntity<ShopDTO> resp = restTemplate.exchange(
                    URLShop + "/" + shopId + "?token=" + token,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    ShopDTO.class);
            return resp.getBody() != null
                    ? resp.getBody().getName()
                    : "Unknown Shop";
        } catch (Exception e) {
            log.warn("Error fetching shop name for id {}", shopId, e);
            return "Unknown Shop";
        }
    }

    private String fetchItemName(int shopId, BidRecieptDTO bid) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String url   = baseUrl + "/shops/" + shopId + "/items?token=" + token;
        try {
            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                url, HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class
            );
            JsonNode body = resp.getBody();

            if (body != null && body.isArray() && body.size() > 0) {
                // 1) If our DTO map is empty, just grab the first element's name:
                if (bid.getItems().isEmpty()) {
                    JsonNode first = body.get(0);
                    int    fid   = first.path("id").asInt(-1);
                    String fname = first.path("name").asText("(no-name)");
                    return fname;
                }

                // 2) Otherwise do your normal matching:
                for (JsonNode item : body) {
                    int    id   = item.path("id").asInt(-1);
                    String name = item.path("name").asText("(no-name)");
                    if (bid.getItems().containsKey(id)) {
                        return name;
                    }
                }
            } else {
            }
        } catch (Exception e) {
            /*ignore */
        }
        return "";
    }

    private void payForBid(BidRecieptDTO dto) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Purchase Summary");
        ShoppingCartDTO cartDto = dto.toShopingCartDTO(baseUrl);
        PurchaseCompletionIntermidiate purchaseCompletion = new PurchaseCompletionIntermidiate(baseUrl, cartDto,
                dialog, dto.getStoreId(), dto.getHighestBid());

        // Add your component to the dialog
        dialog.add(purchaseCompletion);
        // Add your component to the dialog
        dialog.add(purchaseCompletion);

        // Optional: add a close button in the footer
        Button closeButton = new Button("Close", ev -> dialog.close());
        dialog.getFooter().add(closeButton);

        dialog.open(); // Show the dialog
        buildView(); // Refresh the view after purchase
    }


    private boolean isGuest() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            Notification.show("No auth token found. Please log in.", 3000, Notification.Position.MIDDLE);
            return true; // Default to guest if not authenticated
        }

        String url = URLUser + "/isGuest?token=" + token;

        try {
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                Notification.show("Could not determine guest status", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            Notification.show("Error checking guest status", 3000, Notification.Position.MIDDLE);
        }

        return true; // fallback to guest on error
    }
}
