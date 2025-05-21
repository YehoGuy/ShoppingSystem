package UI;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import DTOs.CartEntryDTO;
import DTOs.ItemDTO;
import DTOs.ShoppingCartDTO;
import DTOs.ShopDTO;

@Route(value = "cart", layout = AppLayoutBasic.class)
public class ShoppingCartView extends VerticalLayout implements BeforeEnterObserver {
    private ShoppingCartDTO cart;
    private List<ShopDTO> shops;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String URLShop = "http://localhost:8080/api/shops";
    private static final String URLUser = "http://localhost:8080/api/users";
    private static final String URLPurchases = "http://localhost:8080/api/purchases";
    private static final String URLItem = "http://localhost:8080/api/items";

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    public ShoppingCartView() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        getData();

        H1 title = new H1("Shopping cart");

        Div buyButtonContainer = new Div();
        buyButtonContainer.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("right", "0")
                .set("margin", "80px")
                .set("padding", "10px");

        Button buyButton = new Button("Buy entire cart");
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
                int quantity = itemQuantitiesMap != null && itemQuantitiesMap.containsKey(itemId) ? itemQuantitiesMap.get(itemId) : 1;
                double price = itemPricesMap != null && itemPricesMap.containsKey(itemId) ? itemPricesMap.get(itemId) : 0;
                shopTotal += price * quantity;
                ItemDTO item = getAllItems().stream().filter(it -> it.getId() == itemId).findFirst().orElse(null);
                if (item != null) {
                    entries.add(new CartEntryDTO(quantity, item, shopID));
                }
            }

            Button buyBasketButton = new Button("Buy basket from " + shopName + " " + shopTotal + "₪");
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
                Double price = itemPricesMap != null && itemPricesMap.containsKey(item.getId()) ? itemPricesMap.get(item.getId()) : 0;
                rows.add(new ItemRow(item, quantity, price));
            }

            grid.setItems(rows);
            grid.addComponentColumn(renderer -> {
                Button removeButton = new Button(VaadinIcon.MINUS.create());
                Button addButton = new Button(VaadinIcon.PLUS.create());
                Button removeCompletlyButton = new Button(VaadinIcon.TRASH.create());
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
            if (itemQuantities == null) continue;
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
            String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(null, headers);
            String url = URLItem + "/all";
            Map<String, String> params = new HashMap<>();
            params.put("authToken", token);
            ResponseEntity<?> response = restTemplate.getForEntity(url, List.class, entity, params);
            if (response.getStatusCode().is2xxSuccessful()) {
                List<ItemDTO> items = (List<ItemDTO>) response.getBody();
                if (items != null) {
                    return items;
                }
            }
        } catch (Exception e) {
            Notification.show("Error: could not retrieve shopping cart", 5000,
                    Notification.Position.MIDDLE);
        }
        return Collections.emptyList();
    }

    private List<ShopDTO> getShopNames(Set<Integer> keySet) {
        List<ShopDTO> result = new ArrayList<>();
        try {
            String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
            for (int i : keySet) {
                String url = URLShop + "/" + i;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<?> entity = new HttpEntity<>(null, headers);
                Map<String, String> params = new HashMap<>();
                params.put("authToken", token);
                ResponseEntity<ShopDTO> response = restTemplate.getForEntity(url, ShopDTO.class, params);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    result.add(response.getBody());
                }
            }
        } catch (Exception e) {
            Notification.show("Error: could not retrieve shop names", 5000,
                    Notification.Position.MIDDLE);
        }
        return result;
    }

    private HashMap<Integer, HashMap<Integer, Integer>> getCartIDs() {
        try {
            String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(null, headers);
            String url = URLUser + "/shoppingCart";
            Map<String, String> params = new HashMap<>();
            params.put("authToken", token);
            ResponseEntity<?> response = restTemplate.getForEntity(url, HashMap.class, params);
            if (response.getStatusCode().is2xxSuccessful()) {
                HashMap<Integer, HashMap<Integer, Integer>> cartIDs = (HashMap<Integer, HashMap<Integer, Integer>>) response.getBody();
                if (cartIDs != null) {
                    return cartIDs;
                }
            }
        } catch (Exception e) {
            Notification.show("Error: could not retrieve shopping cart", 5000,
                    Notification.Position.MIDDLE);
        }
        return new HashMap<>();
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
            String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(null, headers);

            // Find itemId by name
            List<Integer> itemIds = cart.getShopItems().get(shopID);
            int itemID = -1;
            for (Integer id : itemIds) {
                ItemDTO item = getAllItems().stream().filter(it -> it.getId() == id && it.getName().equals(itemName)).findFirst().orElse(null);
                if (item != null) {
                    itemID = id;
                    break;
                }
            }
            if (itemID == -1) return;

            String url = URLUser + "/shoppingCart/" + shopID + "/" + itemID + "/" + action;
            Map<String, String> params = new HashMap<>();
            params.put("authToken", token);
            ResponseEntity<?> response = restTemplate.postForEntity(url, entity, String.class, params);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Cart updated", 3000, Notification.Position.BOTTOM_START);
                UI.getCurrent().getPage().reload();
            }
        } catch (Exception e) {
            Notification.show("Error updating cart", 3000, Notification.Position.BOTTOM_START);
        }
    }
}
