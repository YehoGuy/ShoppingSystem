package UI;

import DTOs.*;
import Domain.ItemCategory;
import Domain.PermissionsEnum;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Vertical;

import org.objectweb.asm.Label;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Route(value = "edit-shop", layout = AppLayoutBasic.class)
@JsModule("./js/notification-client.js")
public class EditShopView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String USERS_URL = "http://localhost:8080/api/users";
    private final String SHOPS_URL = "http://localhost:8080/api/shops";
    private final String PERMISSIONS_URL = "http://localhost:8080/api/users/hasPermission";
    private ShopDTO shop;
    private Map<ItemDTO, Integer> allItemPrices;
    private VerticalLayout itemsContainer;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }
        UI.getCurrent().getPage().executeJs("import(./js/notification-client.js).then(m => m.connectNotifications($0))",
                getUserId());
    }

    private String getUserId() {
        return VaadinSession.getCurrent().getAttribute("userId").toString();
    }

    private void loadShopData(int shopId) {
        try {
            String token = getToken();
            String hasRoleUrl = USERS_URL + "/hasRole?token=" + token + "&userId=" + getUserId() + "&shopId=" + shopId;
            ResponseEntity<Boolean> hasRoleResponse = restTemplate.getForEntity(hasRoleUrl, Boolean.class);
            if (hasRoleResponse.getStatusCode() != HttpStatus.OK || !hasRoleResponse.getBody()) {
                Notification.show("You do not have permission to edit this shop.");
                UI.getCurrent().navigate("home");
            }
            String getShopUrl = SHOPS_URL + shopId + "?token=" + token;
            ResponseEntity<ShopDTO> response = restTemplate.getForEntity(getShopUrl, ShopDTO.class);
            String hasPermissionUrl = USERS_URL + "/hasPermission?token=" + token + "&userId=" + getUserId()
                    + "&shopId=" + shopId + "&permission=";
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                this.shop = response.getBody();
                if (restTemplate.getForEntity(hasPermissionUrl + PermissionsEnum.manageItems, Boolean.class)
                        .getBody()) {
                    if (this.shop.getItems() == null) {
                        this.shop.setItems(new ArrayList<>());
                    } else {
                        this.allItemPrices = this.shop.getItems().stream()
                                .collect(HashMap::new,
                                        (map, item) -> map.put(item,
                                                shop.getItemPrices().get(item.getId()) != null
                                                        ? shop.getItemPrices().get(item.getId()).intValue()
                                                        : null),
                                        HashMap::putAll);
                    }
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
        Button addItemButton;
        Button closeShopButton;
        H1 title = new H1("Edit Shop: " + shop.getName());
        add(title);
        String canManageItemUrl = PERMISSIONS_URL + "?token=" + getToken() + "&userId="
                + getUserId() + "&shopId=" + shop.getShopId() + "&permission=" + PermissionsEnum.manageItems;
        if (restTemplate.getForEntity(canManageItemUrl, Boolean.class).getBody()) {
            addItemButton = new Button("Add Item", e -> openAddItemDialog());
        } else {
            addItemButton = new Button("Add Item", e -> Notification.show("You do not have permission to add items."));
        }
        String canCloseShopUrl = PERMISSIONS_URL + "?token=" + getToken() + "&userId="
                + getUserId() + "&shopId=" + shop.getShopId() + "&permission=" + PermissionsEnum.closeShop;
        if (restTemplate.getForEntity(canCloseShopUrl, Boolean.class).getBody()) {
            closeShopButton = new Button("Close Shop", e -> {
                String token = getToken();
                String url = "http://localhost:8080/api/shops/" + shop.getShopId() + "?token=" + token;
                ResponseEntity<Void> response = restTemplate.postForEntity(url, null, Void.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    Notification.show("Shop closed successfully");
                    UI.getCurrent().navigate("home");
                } else {
                    Notification.show("Failed to close shop");
                }
            });
        } else {
            closeShopButton = new Button("Close Shop",
                    e -> Notification.show("You do not have permission to close the shop."));
        }
        add(addItemButton, closeShopButton);

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

        if (restTemplate.getForEntity(USERS_URL + "/hasPermission?token=" + getToken() + "&userId="
                + getUserId() + "&shopId=" + shop.getShopId() + "&permission=" + PermissionsEnum.manageItems,
                Boolean.class).getBody()) {
            if (allItemPrices == null || allItemPrices.isEmpty()) {
                itemsContainer.add(new Span("No items found."));
                return;
            }

            for (ItemDTO item : allItemPrices.keySet()) {
                Button addSupply, deleteItem, removeSupply, editPrice, setItemDiscount, removeItemDiscount;
                HorizontalLayout itemLayout = new HorizontalLayout();
                itemLayout.setWidthFull();
                itemLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                itemLayout.getStyle().set("border", "1px solid #ccc");
                itemLayout.getStyle().set("padding", "10px");
                itemLayout.getStyle().set("border-radius", "8px");
                itemLayout.getStyle().set("margin-bottom", "10px");

                Span itemName = new Span("Item: " + item.getName());
                Span itemPrice = new Span("Price: " + allItemPrices.get(item) + " $");
                Span itemCategory = new Span("Category: " + item.getCategory());
                Span itemDescription = new Span("Description: " + item.getDescription());
                Span itemRating = new Span("Rating: " + item.getAverageRating());
                VerticalLayout itemDetails = new VerticalLayout(itemName, itemPrice, itemCategory, itemDescription,
                        itemRating);
                itemDetails.setWidth("70%");

                String canManageItemsUrl = PERMISSIONS_URL + "?token=" + getToken() + "&userId="
                        + getUserId() + "&shopId=" + shop.getShopId() + "&permission="
                        + PermissionsEnum.manageItems;
                if (restTemplate.getForEntity(canManageItemsUrl, Boolean.class).getBody()) {
                    addSupply = new Button("Add Supply", e -> {
                        Dialog supplyDialog = new Dialog();
                        TextField supplyQuantity = new TextField("Supply Quantity");
                        Button confirmSupply = new Button("Confirm", evt -> {
                            String token = getToken();
                            String url = "http://localhost:8080/api/shops/" + shop.getShopId() + "/items/"
                                    + item.getId() + "/supply?quantity=" + supplyQuantity.getValue()
                                    + "&token=" + token;
                            ResponseEntity<Void> supplyResponse = restTemplate.postForEntity(url, null, Void.class);
                            if (supplyResponse.getStatusCode() == HttpStatus.OK) {
                                Notification.show("Supply added successfully");
                                loadShopData(shop.getShopId());
                                displayItems();
                            } else {
                                Notification.show("Failed to add supply");
                            }
                            supplyDialog.close();
                        });
                        supplyDialog.add(new VerticalLayout(supplyQuantity, confirmSupply));
                        supplyDialog.open();
                    });
                    deleteItem = new Button("Delete Item", e -> {
                        String token = getToken();
                        String url = "http://localhost:8080/api/shops/" + shop.getShopId() + "/items/" + item.getId()
                                + "?token=" + token;
                        ResponseEntity<Void> deleteResponse = restTemplate.exchange(url, HttpMethod.DELETE, null,
                                Void.class);
                        if (deleteResponse.getStatusCode() == HttpStatus.NO_CONTENT) {
                            Notification.show("Item deleted successfully");
                            loadShopData(shop.getShopId());
                            displayItems();
                        } else {
                            Notification.show("Failed to delete item");
                        }
                    });
                    removeSupply = new Button("Remove Supply", e -> {
                        Dialog removeSupplyDialog = new Dialog();
                        TextField removeQuantity = new TextField("Remove Quantity");
                        Button confirmRemove = new Button("Confirm", evt -> {
                            String token = getToken();
                            String url = "http://localhost:8080/api/shops/" + shop.getShopId() + "/items/"
                                    + item.getId() + "/supply/remove?supply=" + removeQuantity.getValue()
                                    + "&token=" + token;
                            ResponseEntity<Void> removeResponse = restTemplate.postForEntity(url, null, Void.class);
                            if (removeResponse.getStatusCode() == HttpStatus.OK) {
                                Notification.show("Supply removed successfully");
                                loadShopData(shop.getShopId());
                                displayItems();
                            } else {
                                Notification.show("Failed to remove supply");
                            }
                            removeSupplyDialog.close();
                        });
                        removeSupplyDialog.add(new VerticalLayout(removeQuantity, confirmRemove));
                        removeSupplyDialog.open();
                    });
                    editPrice = new Button("Edit Price", e -> {
                        Dialog priceDialog = new Dialog();
                        NumberField newPriceField = new NumberField("New Price");
                        Button confirmPrice = new Button("Confirm", evt -> {
                            String token = getToken();
                            String url = "http://localhost:8080/api/shops/" + shop.getShopId() + "/items/"
                                    + item.getId() + "/price?price=" + newPriceField.getValue()
                                    + "&token=" + token;
                            ResponseEntity<Void> priceResponse = restTemplate.postForEntity(url, null, Void.class);
                            if (priceResponse.getStatusCode() == HttpStatus.OK) {
                                Notification.show("Price updated successfully");
                                loadShopData(shop.getShopId());
                                displayItems();
                            } else {
                                Notification.show("Failed to update price");
                            }
                            priceDialog.close();
                        });
                        priceDialog.add(new VerticalLayout(newPriceField, confirmPrice));
                        priceDialog.open();
                    });
                    setItemDiscount = new Button("Set Discount", e -> {
                        Dialog discountDialog = new Dialog();
                        NumberField discountField = new NumberField("Discount Percentage");
                        discountField.setMin(0);
                        discountField.setMax(100);
                        Button confirmDiscount = new Button("Confirm", evt -> {
                            String token = getToken();
                            String url = "http://localhost:8080/api/shops/" + shop.getShopId() + "/discount/items/"
                                    + item.getId() + "/discount=" + discountField.getValue()
                                    + "isDouble=true" + "&token=" + token;
                            ResponseEntity<Void> discountResponse = restTemplate.postForEntity(url, null, Void.class);
                            if (discountResponse.getStatusCode() == HttpStatus.OK) {
                                Notification.show("Discount set successfully");
                                loadShopData(shop.getShopId());
                                displayItems();
                            } else {
                                Notification.show("Failed to set discount");
                            }
                            discountDialog.close();
                        });
                        discountDialog.add(new VerticalLayout(discountField, confirmDiscount));
                        discountDialog.open();
                    });
                    removeItemDiscount = new Button("Remove Discount", e -> {
                        String token = getToken();
                        String url = "http://localhost:8080/api/shops/" + shop.getShopId() + "/discount/items/"
                                + item.getId() + "?token=" + token;
                        ResponseEntity<Void> removeDiscountResponse = restTemplate.exchange(url, HttpMethod.DELETE,
                                null,
                                Void.class);
                        if (removeDiscountResponse.getStatusCode() == HttpStatus.NO_CONTENT) {
                            Notification.show("Discount removed successfully");
                            loadShopData(shop.getShopId());
                            displayItems();
                        } else {
                            Notification.show("Failed to remove discount");
                        }
                    });
                } else {
                    addSupply = new Button("Add Supply",
                            e -> Notification.show("You do not have permission to add supply."));
                    deleteItem = new Button("Delete Item",
                            e -> Notification.show("You do not have permission to delete items."));
                    removeSupply = new Button("Remove Supply",
                            e -> Notification.show("You do not have permission to remove supply."));
                    editPrice = new Button("Edit Price",
                            e -> Notification.show("You do not have permission to edit prices."));
                    setItemDiscount = new Button("Set Discount",
                            e -> Notification.show("You do not have permission to set discounts."));
                    removeItemDiscount = new Button("Remove Discount",
                            e -> Notification.show("You do not have permission to remove discounts."));
                }

                VerticalLayout buttonsLayout = new VerticalLayout(addSupply, deleteItem, removeSupply, editPrice,
                        setItemDiscount, removeItemDiscount);
                buttonsLayout.setWidth("30%");
                itemLayout.add(itemDetails, buttonsLayout);
                itemLayout.setFlexGrow(1, itemDetails);
                itemsContainer.add(itemLayout);
            }
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
