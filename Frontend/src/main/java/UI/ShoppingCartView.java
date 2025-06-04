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
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import DTOs.CartEntryDTO;
import DTOs.ItemDTO;
import DTOs.ShopDTO;
import DTOs.ShoppingCartDTO;

@Route(value = "cart", layout = AppLayoutBasic.class)
@JsModule("./js/notification-client.js")
public class ShoppingCartView extends VerticalLayout implements BeforeEnterObserver {
    private static final Logger log = LoggerFactory.getLogger(ShoppingCartView.class);
    private ShoppingCartDTO cart;
    private List<ShopDTO> shops;

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${url.api}/shops")
    private String URLShop;

    @Value("${url.api}/users")
    private String URLUser;

    @Value("${url.api}/purchases")
    private String URLPurchases;

    @Value("${url.api}/items")
    private String URLItem;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
        UI.getCurrent().getPage().executeJs("import(./js/notification-client.js).then(m -> m.connectNotifications($0))",
                getUserId());
        handleSuspence();
    }

    private String getUserId() {
        return VaadinSession.getCurrent().getAttribute("userId").toString();
    }

    public ShoppingCartView() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        getData();

        if (cart.getShopItems() == null || cart.getShopItems().isEmpty()) {
        H2 empty = new H2("Your shopping cart is empty 😕");
        empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
        add(empty);

        // optional: add a “continue shopping” button
        Button shopMore = new Button("Continue Shopping", e -> 
            UI.getCurrent().navigate("items")  // or whatever your product listing route is
        );
        add(shopMore);

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
            PurchaseCompletionIntermidiate purchaseCompletion = new PurchaseCompletionIntermidiate(cart);
            this.removeAll();
            this.add(purchaseCompletion);
        });
        buyButton.getStyle().set("background-color", "red").set("color", "white");
        buyButtonContainer.add(buyButton);

        add(title);

        // For each shop in the cart
        cart.getShopItems().forEach((shopID, itemIDs) -> {
            String shopName = getShopName(shopID);
            Map<Integer, Double> itemPricesMap = cart.getShopItemPrices().get(shopID);
            Map<Integer, Integer> itemQuantitiesMap = cart.getShopItemQuantities().get(shopID);

            double shopTotal = 0;
            List<CartEntryDTO> entries = new ArrayList<>();
            for (Integer itemId : itemIDs) {
                int quantity = itemQuantitiesMap != null && itemQuantitiesMap.containsKey(itemId)
                        ? itemQuantitiesMap.get(itemId)
                        : 1;
                double price = itemPricesMap != null && itemPricesMap.containsKey(itemId) ? itemPricesMap.get(itemId)
                        : 0;
                shopTotal += price * quantity;
                ItemDTO item = getAllItems().stream().filter(it -> it.getId() == itemId).findFirst().orElse(null);
                if (item != null) {
                    entries.add(new CartEntryDTO(quantity, item, shopID));
                }
            }

            Button buyBasketButton = new Button("Buy basket from " + shopName + " " + shopTotal + "₪");
            if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
                buyBasketButton.setVisible(false);
            }
            buyBasketButton.getStyle().set("background-color", "blue").set("color", "white");
            buyBasketButton.addClickListener(event -> {
                ShoppingCartDTO shopCart = new ShoppingCartDTO();
                shopCart.setShopItems(Map.of(shopID, itemIDs));
                shopCart.setShopItemPrices(Map.of(shopID, itemPricesMap));
                shopCart.setShopItemQuantities(Map.of(shopID, itemQuantitiesMap));
                PurchaseCompletionIntermidiate purchaseCompletion = new PurchaseCompletionIntermidiate(shopCart);
                this.removeAll();
                this.add(purchaseCompletion);
            });
            H3 shopHeader = new H3(shopName + " - total price: " + shopTotal + "₪");
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
                rows.add(new ItemRow(item, quantity, price));
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
                    handleCartAction(shopID, renderer.name(), "remove");
                });
                addButton.addClickListener(event -> {
                    handleCartAction(shopID, renderer.name(), "plus");
                });
                removeButton.addClickListener(event -> {
                    handleCartAction(shopID, renderer.name(), "minus");
                });
                HorizontalLayout buttonLayout = new HorizontalLayout(addButton, removeButton, removeCompletlyButton);
                buttonLayout.setAlignItems(Alignment.CENTER);
                buttonLayout.setSpacing(true);
                return buttonLayout;
            }).setHeader("Actions").setFlexGrow(0).setWidth("200px");
            add(grid);
        });

        HorizontalLayout totalContainer = new HorizontalLayout();
        totalContainer.setAlignItems(Alignment.CENTER);
        totalContainer.getStyle()
                .set("bottom", "0")
                .set("right", "0")
                .set("margin", "0px");
        H2 total = new H2("Total: " + cart.getTotalPrice() + "₪");
        totalContainer.add(total, buyButton);
        add(buyButtonContainer, totalContainer);
    }

    private void getData() {
        HashMap<Integer, HashMap<Integer, Integer>> IDs = getCartIDs();
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

        cart = new ShoppingCartDTO();
        cart.setShopItems(shopItems);
        cart.setShopItemPrices(shopItemPrices);
        cart.setShopItemQuantities(shopItemQuantities);
    }

    private List<ItemDTO> getAllItems() {
        try {
            String token = VaadinSession.getCurrent().getAttribute("authToken").toString();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<ItemDTO>> response = restTemplate.exchange(
                URLItem + "/all?authToken={authToken}",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {},
                token
            );
            List<ItemDTO> items = response.getBody();
            return (items != null) ? items : Collections.emptyList();
        }
        catch (HttpClientErrorException.NotFound nf) {
            // no items ⇒ empty
            log.warn("No items found in the database, returning empty list");
            return Collections.emptyList();
        }
        catch (Exception e) {
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
                    URLShop + "/" + id + "?authToken={authToken}",
                    HttpMethod.GET,
                    entity,
                    ShopDTO.class,
                    token
                );
                if (resp.getBody() != null) {
                    result.add(resp.getBody());
                }
            }
            catch (HttpClientErrorException.NotFound nf) {
                // skip
                log.warn("Shop with ID {} not found, skipping", id);
            }
            catch (Exception e) {
                // skip all other errors silently
                log.warn("Error fetching shop with ID {}: {}", id, e.getMessage());
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
                URLUser + "/shoppingCart?authToken={authToken}",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {},
                token
            );
            HashMap<Integer, HashMap<Integer, Integer>> body = resp.getBody();
            return (body != null) ? body : new HashMap<>();
        }
        catch (HttpClientErrorException.NotFound nf) {
            // no cart yet ⇒ empty
            return new HashMap<>();
        }
        catch (Exception e) {
            // swallow everything else silently
            return new HashMap<>();
        }
    }

    // Helper record class to populate the grid
    public record ItemRow(String name, double price, int quantity, double totalPrice, String description) {
        public ItemRow(ItemDTO item, int quantity, double price) {
            this(item.getName(), price, quantity, price * quantity, item.getDescription());
        }
    }

    private String getShopName(int shopId) {
        return shops.stream()
                .filter(shop -> shop.getShopId() == shopId)
                .map(ShopDTO::getName)
                .findFirst()
                .orElse("Unknown Shop");
    }

    private void handleCartAction(int shopID, String itemName, String action) {
        try {
            String token = VaadinSession.getCurrent().getAttribute("authToken").toString();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // find itemID …
            int itemID = getAllItems().stream()
                .filter(item -> item.getName().equals(itemName))
                .map(ItemDTO::getId)
                .findFirst()
                .orElse(-1);
            if (itemID < 0) return;

            restTemplate.postForEntity(
                URLUser + "/shoppingCart/" + shopID + "/" + itemID + "/" + action + "?authToken={authToken}",
                entity,
                String.class,
                token
            );
            UI.getCurrent().getPage().reload();
        }
        catch (Exception e) {
            // completely silent on failure
            log.warn("Could not retrieve shopping cart, treating as empty", e);
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
