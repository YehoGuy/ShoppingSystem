package UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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

import DTOs.CartEntryDTO;
import DTOs.ItemDTO;
import DTOs.ShoppingCartDTO;
import Domain.ItemCategory;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import DTOs.ShopDTO;


@Route(value = "cart", layout = AppLayoutBasic.class)
public class ShoppingCartView extends VerticalLayout implements BeforeEnterObserver {
    private ShoppingCartDTO cart;
    private Map<String, Map<Integer, Double>> itemPrices;
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
        // Mock data for demonstration purposes
        // In a real application, you would retrieve this data from a service or
        // database
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
            PurchaseCompletionIntermediate purchaseCompletion = new PurchaseCompletionIntermediate(cart);
            this.removeAll();
            this.add(purchaseCompletion);});
        buyButton.getStyle().set("background-color", "red").set("color", "white");
        buyButtonContainer.add(buyButton);

        add(title);

        cart.getCartItems().forEach((shopName, items) -> {
            double shopTotal = cart.getShopPrices().getOrDefault(shopName, 0.0);
            Button buyBasketButton = new Button("Buy basket from " + shopName);
            buyBasketButton.getStyle().set("background-color", "blue").set("color", "white");
            buyBasketButton.addClickListener(event -> {
                ShoppingCartDTO shopCart = new ShoppingCartDTO();
                shopCart.setCartItems(Map.of(shopName, items));
                shopCart.setTotalPrice(shopTotal);
                shopCart.setShopPrices(Map.of(shopName, shopTotal));
                PurchaseCompletionIntermediate purchaseCompletion = new PurchaseCompletionIntermediate(shopCart);
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
            // Convert items to list of rows
            List<ItemRow> rows = new ArrayList<>();
            for(int i = 0; i < items.size(); i++) {
                CartEntryDTO entry = items.get(i);
                ItemDTO item = entry.getItem();
                int quantity = entry.getQuantity();
                double price = cart.getShopPrices().getOrDefault(item.getId(), 0.0);
                rows.add(new ItemRow(item, quantity, price));
            }

            grid.setItems(rows);
            grid.addComponentColumn(renderer -> {
                Button removeButton = new Button(VaadinIcon.MINUS.create());
                Button addButton = new Button(VaadinIcon.PLUS.create());
                Button removeCompletlyButton = new Button(VaadinIcon.TRASH.create());
                removeCompletlyButton.addClickListener(event -> {
                    try {
                        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<?> entity = new HttpEntity<>(null, headers);
                        int itemID = cart.getCartItems().get(shopName).stream()
                                .filter(item -> item.getItem().getName().equals(renderer.name()))
                                .findFirst()
                                .map(CartEntryDTO::getItemId)
                                .orElse(-1);
                        int shopID = shops.stream()
                                .filter(shop -> shop.getName().equals(shopName))
                                .findFirst()
                                .map(ShopDTO::getShopId)
                                .orElse(-1);
                        String url = URLUser + "/shoppingCart/" + shopID + "/" + itemID + "/remove";
                        Map<String, String> params = new HashMap<>();
                        params.put("authToken", token);
                        ResponseEntity<?> response = restTemplate.postForEntity(url, entity, String.class, params);
                        if (response.getStatusCode().is2xxSuccessful()) {
                            Notification.show("Item removed from cart", 3000, Notification.Position.BOTTOM_START);
                            UI.getCurrent().getPage().reload();
                        }
                    } catch (Exception e) {
                        Notification.show("Error removing item from cart", 3000, Notification.Position.BOTTOM_START);
                    }
                });
                addButton.addClickListener(event -> {
                    try {
                        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<?> entity = new HttpEntity<>(null, headers);
                        int itemID = cart.getCartItems().get(shopName).stream()
                                .filter(item -> item.getItem().getName().equals(renderer.name()))
                                .findFirst()
                                .map(CartEntryDTO::getItemId)
                                .orElse(-1);
                        int shopID = shops.stream()
                                .filter(shop -> shop.getName().equals(shopName))
                                .findFirst()
                                .map(ShopDTO::getShopId)
                                .orElse(-1);
                        String url = URLUser + "/shoppingCart/" + shopID + "/" + itemID + "/plus";
                        Map<String, String> params = new HashMap<>();
                        params.put("authToken", token);
                        ResponseEntity<?> response = restTemplate.postForEntity(url, entity, String.class, params);
                        if (response.getStatusCode().is2xxSuccessful()) {
                            Notification.show("Item added to cart", 3000, Notification.Position.BOTTOM_START);
                            UI.getCurrent().getPage().reload();
                        }
                    } catch (Exception e) {
                        Notification.show("Error adding item to cart", 3000, Notification.Position.BOTTOM_START);
                    }
                });
                removeButton.addClickListener(event -> {
                    try {
                        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<?> entity = new HttpEntity<>(null, headers);
                        int itemID = cart.getCartItems().get(shopName).stream()
                                .filter(item -> item.getItem().getName().equals(renderer.name()))
                                .findFirst()
                                .map(CartEntryDTO::getItemId)
                                .orElse(-1);
                        int shopID = shops.stream()
                                .filter(shop -> shop.getName().equals(shopName))
                                .findFirst()
                                .map(ShopDTO::getShopId)
                                .orElse(-1);
                        String url = URLUser + "/shoppingCart/" + shopID + "/" + itemID + "/minus";
                        Map<String, String> params = new HashMap<>();
                        params.put("authToken", token);
                        ResponseEntity<?> response = restTemplate.postForEntity(url, entity, String.class, params);
                        if (response.getStatusCode().is2xxSuccessful()) {
                            Notification.show("Item removed from cart", 3000, Notification.Position.BOTTOM_START);
                            UI.getCurrent().getPage().reload();
                        }
                    } catch (Exception e) {
                        Notification.show("Error removing item from cart", 3000, Notification.Position.BOTTOM_START);
                    }
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
        Map<String, List<CartEntryDTO>> cartItems = new HashMap<>();
        double totalPrice = 0.0;
        Map<String, Double> shopPrices = new HashMap<>();
        itemPrices = new HashMap<>();
        for (int i = 0; i < shops.size(); i++) {
            int shopId = shops.get(i).getShopId();
            String shopName = shops.get(i).getName();
            List<CartEntryDTO> cartEntries = new ArrayList<>();
            List<Double> prices = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : IDs.get(IDs.keySet().stream().toList().get(i)).entrySet()) {
                int itemId = entry.getKey();
                int quantity = entry.getValue();
                ItemDTO item = items.stream().filter(it -> it.getId() == itemId).findFirst().orElse(null);
                if (item != null) {
                    cartEntries.add(new CartEntryDTO(itemId, quantity, item));
                }
                prices.add(shops.get(i).getItemPrices().get(itemId));
            }
            cartItems.put(shopName, cartEntries);
            double shopTotal = prices.stream().mapToDouble(Double::doubleValue).sum();
            shopPrices.put(shopName, shopTotal);
            itemPrices.put(shopName, new HashMap<>());
            for (int j = 0; j < cartEntries.size(); j++) {
                itemPrices.get(shopName).put(cartEntries.get(j).getItemId(), prices.get(j));
            }
            totalPrice += shopTotal;
        }
        cart = new ShoppingCartDTO(cartItems, totalPrice, shopPrices);
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
        return null;
    }

    private List<ShopDTO> getShopNames(Set<Integer> keySet) {
        try {
            String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
            for (int i : keySet) {
                String url = URLShop + "/" + i;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<?> entity = new HttpEntity<>(null, headers);
                Map<String, String> params = new HashMap<>();
                params.put("shopsIds", keySet.toString());
                params.put("authToken", token);
                ResponseEntity<?> response = restTemplate.getForEntity(url, List.class, entity, params);
                if (response.getStatusCode().is2xxSuccessful()) {
                    List<ShopDTO> shops = (List<ShopDTO>) response.getBody();
                    if (shops != null) {
                        return shops;
                    }
                }
            }
            
        } catch (Exception e) {
            Notification.show("Error: could not retrieve shopping cart", 5000,
                    Notification.Position.MIDDLE);
        }
        return null;
    }

    private HashMap<Integer, HashMap<Integer, Integer>> getCartIDs() {
        try{
            String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(null,headers);
            String url = URLUser + "/shoppingCart";
            Map<String, String> params = new HashMap<>();
            params.put("authToken", token);
            ResponseEntity<?> response = restTemplate.getForEntity(url, HashMap.class, params);
            if (response.getStatusCode().is2xxSuccessful()) {
                HashMap<Integer, HashMap<Integer, Integer>> cartIDs = (HashMap<Integer, HashMap<Integer, Integer>>) response.getBody();
                if(cartIDs != null) {
                    return cartIDs;
                }
            }
        }
        catch (Exception e) {
            Notification.show("Error: could not retrieve shopping cart", 5000,
                    Notification.Position.MIDDLE);
        }

        return null;
    }

    // Helper record class to populate the grid
    public record ItemRow(String name, double price, int quantity, double totalPrice, String description) {
        public ItemRow(ItemDTO item, int quantity, double price) {
            this(item.getName(), price, quantity, price * quantity, item.getDescription());
        }
    }
}