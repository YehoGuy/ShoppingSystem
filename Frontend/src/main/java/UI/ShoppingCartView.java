package UI;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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

import DTOs.ItemDTO;
import DTOs.ShoppingCartDTO;
import Domain.ItemCategory;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

@Route(value = "cart", layout = AppLayoutBasic.class)
public class ShoppingCartView extends VerticalLayout implements BeforeEnterObserver {

    private static final String USER_API_URL = "http://localhost:8080/api/users";
    private static final String SHOP_API_URL = "http://localhost:8080/api/shops";
    private static final String ITEM_API_URL = "http://localhost:8080/api/items";
    private ShoppingCartDTO shoppingCart;
    private final RestTemplate restTemplate = new RestTemplate();

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
        getShoppingCart();
        setSizeFull();
        setSpacing(true);
        setPadding(true);
        setAlignItems(Alignment.CENTER);

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
            // TODO: connect to backend and buy the entire cart
            System.out.println("Buying entire cart...");
        });
        buyButton.getStyle().set("background-color", "red").set("color", "white");
        buyButtonContainer.add(buyButton);

        add(title);

        shoppingCart.getCartItems().forEach((shopName, items) -> {
            Button buyBasketButton = new Button("Buy basket from " + shopName);
            buyBasketButton.getStyle().set("background-color", "blue").set("color", "white");
            buyBasketButton.addClickListener(event -> {
                // TODO: think if we want a buy basket for a single shop option, i dont think we
                // want it
                System.out.println("Buying basket from " + shopName + "...");
            });
            VerticalLayout shopHeaderContainer = new VerticalLayout(buyBasketButton);
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
            List<ItemRow> rows = items.entrySet().stream()
                    .map(e -> new ItemRow(e.getKey(), e.getValue()))
                    .toList();

            grid.setItems(rows);
            grid.addComponentColumn(renderer -> {
                // TODO: we need to think if we want to connect from here to the backend or not
                // it can be possible to not connect to the backend and just remove the item
                // from the
                // cart, but we need to check that the quantities exist in the shops
                Button removeButton = new Button(VaadinIcon.MINUS.create());
                Button addButton = new Button(VaadinIcon.PLUS.create());
                Button removeCompletlyButton = new Button(VaadinIcon.TRASH.create());
                removeCompletlyButton.addClickListener(event -> {
                    System.out.println("Removing all of " + renderer.name() + " from cart...");
                });
                addButton.addClickListener(event -> {
                    System.out.println("Adding " + renderer.name() + " to cart...");
                });
                removeButton.addClickListener(event -> {
                    System.out.println("Removing " + renderer.name() + " from cart...");
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
        totalContainer.add(buyButton);
        add(buyButtonContainer, totalContainer);
    }

    // Helper record class to populate the grid
    public record ItemRow(String name, double price, int quantity, double totalPrice, String description) {
        public ItemRow(ItemDTO item, int quantity) {
            this(item.getName(), item.getPrice(), quantity, item.getPrice() * quantity, item.getDescription());
        }
    }

    private void getShoppingCart() {
        HashMap<Integer, HashMap<Integer, Integer>> cart = getCartItems();
        List<Integer> shopsIds = cart.keySet().stream().toList();
        createShoppingCart(getShopsNames(shopsIds), getAllItems(), cart);
    }

    @SuppressWarnings("unchecked")
    private HashMap<Integer, HashMap<Integer, Integer>> getCartItems() {
        try {
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> request = new HttpEntity<>(null, headers);
            String url = USER_API_URL + "/shoppingCart";
            Map<String, String> params = new HashMap<>();
            params.put("token", authToken);
            ResponseEntity<?> response = restTemplate
                    .getForEntity(url, HashMap.class, request, params);
            if (response.getStatusCode().is2xxSuccessful()) {
                HashMap<Integer, HashMap<Integer, Integer>> responseBody = (HashMap<Integer, HashMap<Integer, Integer>>) response
                        .getBody();
                if (responseBody != null) {
                    return responseBody;
                }
            }
        } catch (Exception e) {
            Notification.show("Error: could not retrieve shopping cart", 5000,
                    Notification.Position.MIDDLE);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getShopsNames(List<Integer> shopsIds) {
        try {
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> request = new HttpEntity<>(null, headers);
            String url = SHOP_API_URL + "/getShopsNames";
            Map<String, String> params = new HashMap<>();
            params.put("shopsIds", shopsIds.toString());
            params.put("token", authToken);
            ResponseEntity<?> response = restTemplate
                    .getForEntity(url, List.class, request, params);
            if (response.getStatusCode().is2xxSuccessful()) {
                List<String> shopsNames = (List<String>) response.getBody();
                if (shopsNames != null) {
                    return shopsNames;
                }
            }
        } catch (Exception e) {
            Notification.show("Error: could not retrieve shopping cart", 5000,
                    Notification.Position.MIDDLE);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<ItemDTO> getAllItems() {
        try {
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> request = new HttpEntity<>(null, headers);
            String url = ITEM_API_URL + "/all";
            Map<String, String> params = new HashMap<>();
            params.put("token", authToken);
            ResponseEntity<?> response = restTemplate
                    .getForEntity(url, List.class, request, params);
            if (response.getStatusCode().is2xxSuccessful()) {
                List<ItemDTO> itemsNames = (List<ItemDTO>) response.getBody();
                if (itemsNames != null) {
                    return itemsNames;
                }
            }
        } catch (Exception e) {
            Notification.show("Error: could not retrieve shopping cart", 5000,
                    Notification.Position.MIDDLE);
        }
        return null;
    }

    private void createShoppingCart(List<String> shopsNames, List<ItemDTO> itemsNames,
            HashMap<Integer, HashMap<Integer, Integer>> cart) {
        // create shopping cart
        Map<String, Map<ItemDTO, Integer>> cartItems = new HashMap<>();
        int i = 0;
        for (String name : shopsNames) {
            HashMap<ItemDTO, Integer> items = new HashMap<>();
            for (Integer itemId : cart.get(i).keySet()) {
                ItemDTO item = itemsNames.stream()
                        .filter(i1 -> i1.getId() == itemId)
                        .findFirst()
                        .orElse(null);
                if (item != null) {
                    items.put(item, cart.get(i).get(itemId));
                }
            }
            cartItems.put(name, items);
            i++;
        }
        shoppingCart = new ShoppingCartDTO(cartItems);
    }
}