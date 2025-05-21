package UI;

import DTOs.*;
import Domain.ItemCategory;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Route(value = "edit-shop", layout = AppLayoutBasic.class)
public class EditShopView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {

    private final RestTemplate restTemplate = new RestTemplate();
    private ShopDTO shop;
    private Map<ItemDTO,Integer> allItemPrices;
    private VerticalLayout itemsContainer;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }
    }

    private void loadShopData(int shopId) {
        try {
            String token = getToken();
            String url = "http://localhost:8080/api/shops/" + shopId + "?token=" + token;
            ResponseEntity<ShopDTO> response = restTemplate.getForEntity(url, ShopDTO.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                this.shop = response.getBody();
                if (this.shop.getItems() == null ) {
                    this.shop.setItems(new ArrayList<>());
                    
                }else{
                    this.allItemPrices = this.shop.getItems().stream()
                        .collect(HashMap::new, (map, item) -> map.put(item, shop.getItemPrices().get(item.getId()) != null ? shop.getItemPrices().get(item.getId()).intValue() : null), HashMap::putAll);
                }

            } else {
                Notification.show("Failed to load shop: " + response.getStatusCode());
            }
        } catch (Exception e) {
            Notification.show("Error loading shop: " + e.getMessage());
        }
    }

    private void buildUI() {
        removeAll();

        H1 title = new H1("Edit Shop: " + shop.getName());
        add(title);

        Button addItemButton = new Button("Add Item", e -> openAddItemDialog());
        add(addItemButton);

        itemsContainer = new VerticalLayout();
        add(itemsContainer);
        displayItems();
    }

    private void openAddItemDialog() {
        Dialog dialog = new Dialog();

        TextField name = new TextField("Name");
        TextField desc = new TextField("Description");
        NumberField price = new NumberField("Price");
        NumberField quantity = new NumberField("Quantity");
        ComboBox<ItemCategory> category = new ComboBox<>("Category");
        category.setItems(ItemCategory.values());

        Button confirm = new Button("Add", e -> {
            String token = getToken();
            String url = "http://localhost:8080/api/shops/" + shop.getShopId() + "/items"
                    + "?name=" + name.getValue()
                    + "&description=" + desc.getValue()
                    + "&quantity=" + quantity.getValue().intValue()
                    + "&price=" + price.getValue().intValue()
                    + "&token=" + token;

            ResponseEntity<Void> response = restTemplate.postForEntity(url, null, Void.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
                Notification.show("Item added");
                loadShopData(shop.getShopId());
                displayItems();
                dialog.close();
            } else {
                Notification.show("Failed to add item");
            }
        });

        dialog.add(new VerticalLayout(name, desc, price, quantity, category, confirm));
        dialog.open();
    }

    private void displayItems() {
        itemsContainer.removeAll();

        if (allItemPrices == null || allItemPrices.isEmpty()) {
            itemsContainer.add(new Span("No items found."));
            return;
        }

        for (ItemDTO item : allItemPrices.keySet()) {
            Span label = new Span(item.getName() + " - " + allItemPrices.get(item));
            itemsContainer.add(label);
        }
    }

    private String getToken() {
        return (String) VaadinSession.getCurrent().getAttribute("authToken");
    }

    @Override
    public void setParameter(BeforeEvent arg0, Integer arg1) {
        loadShopData(arg1);
        buildUI();
    }
}
