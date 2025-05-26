package UI;

import DTOs.*;
import Domain.ItemCategory;
import Domain.PermissionsEnum;
import UI.EditShopView.UserPermissionsDTO;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
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
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Vertical;

import org.objectweb.asm.Label;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "edit-shop", layout = AppLayoutBasic.class)
@JsModule("./js/notification-client.js")
public class EditShopView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String USERS_URL = "http://localhost:8080/api/users";
    private final String SHOPS_URL = "http://localhost:8080/api/shops/";    
    private final String PERMISSIONS_URL = "http://localhost:8080/api/users/hasPermission";
    private ShopDTO shop;
    private Map<ItemDTO, Integer> allItemPrices;
    private VerticalLayout itemsContainer;
    private VerticalLayout rolesLayout;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }
        UI.getCurrent().getPage().executeJs("import(./js/notification-client.js).then(m => m.connectNotifications())",
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
        rolesLayout = new VerticalLayout();
        add(rolesLayout);
        DisplayRoles();
    }

    private void DisplayRoles() {
        Map<Integer, PermissionsEnum[]> roles = getRoles();
        H2 rolesTitle = new H2("Roles and Permissions");
        rolesLayout.removeAll();
        rolesLayout.add(rolesTitle);
        if (roles.isEmpty()) {
            rolesLayout.add(new Span("No roles found."));
            return;
        }
        List<MemberDTO> members = getShopWorkers(shop.getShopId());
        List<UserPermissionsDTO> userPermissionsList = new ArrayList<>();
        for (MemberDTO member : members) {
            PermissionsEnum[] permissions = roles.get(member.getMemberId());
            if (permissions != null) {
                userPermissionsList
                        .add(new UserPermissionsDTO(member.getMemberId(), member.getUsername(), permissions));
            }
        }
        if (userPermissionsList.isEmpty()) {
            rolesLayout.add(new Span("No users with roles found."));
            return;
        }
        Grid<UserPermissionsDTO> rolesGrid = new Grid<>(UserPermissionsDTO.class, false);
        rolesGrid.addColumn(UserPermissionsDTO::getUsername).setHeader("Username").setFlexGrow(1);
        rolesGrid.addColumn(UserPermissionsDTO::getRoleName).setHeader("Role").setFlexGrow(1);
        rolesGrid.addColumn(UserPermissionsDTO::showPermissions).setHeader("Permissions").setFlexGrow(2);

        rolesGrid.addComponentColumn(dto -> {
            Button changeBtn = new Button("Change Permissions");
            changeBtn.addClickListener(e -> {
                changePermissions(dto);
            });
            return changeBtn;
        }).setHeader("Change");

        // Remove button
        rolesGrid.addComponentColumn(dto -> {
            Button removeBtn = new Button("Remove");
            removeBtn.addClickListener(e -> {
                removeMemberFromShop(dto);
            });
            return removeBtn;
        }).setHeader("Remove");

        add(rolesGrid);

        Button addManager = new Button("Add Manager", e -> {
            Dialog dialog = new Dialog();
            ComboBox<String> usernameField = new ComboBox<>("Username");
            List<MemberDTO> notWorkingMembers = getNotWorkingMembers(members);
            usernameField.setItems(getUserNames(notWorkingMembers));
            usernameField.setPlaceholder("Select a user");
            usernameField.setClearButtonVisible(true);
            CheckboxGroup<PermissionsEnum> checkboxGroup = new CheckboxGroup<>();
            checkboxGroup.setLabel("Select Permissions");
            checkboxGroup.setItems(PermissionsEnum.values());
            Button confirmButton = new Button("Confirm", evt -> {
                String username = usernameField.getValue();
                if (username == null || username.isEmpty()) {
                    Notification.show("Please select a user.");
                    return;
                }
                PermissionsEnum[] selectedPermissions = checkboxGroup.getValue().toArray(new PermissionsEnum[0]);
                if (selectedPermissions.length == 0) {
                    Notification.show("Please select at least one permission.");
                    return;
                }
                String url = USERS_URL + "/shops/" + shop.getShopId() + "/managers?memberId="
                        + getSelectedMemberId(username, notWorkingMembers)
                        + "&token=" + getToken() + "&permissions=" + selectedPermissions.toString();
                ResponseEntity<Void> response = restTemplate.postForEntity(url, null, Void.class);
                if (response.getStatusCode() == HttpStatus.CREATED) {
                    Notification.show(username + " got a new role in his pending roles.");
                } else {
                    Notification.show("Failed to add role to user's pending roles: " + response.getStatusCode());
                }
                dialog.close();
            });
            dialog.add(new VerticalLayout(usernameField, checkboxGroup, confirmButton));
            dialog.open();
        });
        rolesLayout.add(addManager);
    }

    private void removeMemberFromShop(UserPermissionsDTO dto) {
        Dialog dialog = new Dialog();
        Button confirmButton = new Button("Confirm", evt -> {
            String url = USERS_URL + "/shops/" + shop.getShopId() + "/managers?memberId=" + dto.getMemberId()
                    + "&token=" + getToken();
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                Notification.show(dto.getUsername() + " was removed from the shop.");
                DisplayRoles();
            } else {
                Notification.show("Failed to remove user from the shop: " + response.getStatusCode());
            }
            dialog.close();
        });
        dialog.add(new VerticalLayout(new Span("Are you sure you want to remove " + dto.getUsername() + "?"),
                confirmButton));
        dialog.open();
    }

    private void changePermissions(UserPermissionsDTO dto) {
        Dialog dialog = new Dialog();
        CheckboxGroup<PermissionsEnum> checkboxGroup = new CheckboxGroup<>();
        checkboxGroup.setLabel("Select Permissions");
        checkboxGroup.setItems(PermissionsEnum.values());
        Button confirmButton = new Button("Confirm", evt -> {
            PermissionsEnum[] selectedPermissions = checkboxGroup.getValue().toArray(new PermissionsEnum[0]);
            if (selectedPermissions.length == 0) {
                Notification.show("Please select at least one permission.");
                return;
            }
            String url = USERS_URL + "/shops/" + shop.getShopId() + "/managers?memberId=" + dto.getMemberId()
                    + "&token=" + getToken() + "&permissions=" + selectedPermissions.toString();
            ResponseEntity<Void> response = restTemplate.postForEntity(url, null, Void.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
                Notification.show(dto.getUsername() + "' permissions were changed.");
            } else {
                Notification.show("Failed to change permissions to user's pending roles: " + response.getStatusCode());
            }
            dialog.close();
        });
        dialog.add(new VerticalLayout(checkboxGroup, confirmButton));
        dialog.open();
    }

    private int getSelectedMemberId(String username, List<MemberDTO> notWorkingMembers) {
        return notWorkingMembers.stream()
                .filter(member -> member.getUsername().equals(username))
                .findFirst()
                .map(MemberDTO::getMemberId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private Collection<String> getUserNames(List<MemberDTO> notWorkingMembers) {
        return notWorkingMembers.stream()
                .map(MemberDTO::getUsername)
                .collect(Collectors.toList());
    }

    private List<MemberDTO> getNotWorkingMembers(List<MemberDTO> workingMembers) {
        String url = USERS_URL + "/allmembers?token=" + getToken();
        ResponseEntity<List<MemberDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<MemberDTO>>() {
                });
        if (response.getStatusCode() == HttpStatus.OK) {
            List<MemberDTO> allMembers = response.getBody();
            if (allMembers == null) {
                return new ArrayList<>();
            }
            return allMembers.stream()
                    .filter(member -> workingMembers.stream().noneMatch(m -> m.getMemberId() == member.getMemberId()))
                    .toList();
        } else {
            Notification.show("Failed to load members: " + response.getStatusCode());
            return new ArrayList<>();
        }
    }

    private List<MemberDTO> getShopWorkers(int shopId) {
        String token = getToken();
        String url = USERS_URL + "/shops/" + shopId + "/workers?token=" + token;
        ResponseEntity<List<MemberDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<MemberDTO>>() {
                });

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            Notification.show("Failed to load shop workers: " + response.getStatusCode());
            return new ArrayList<>();
        }
    }

    private Map<Integer, PermissionsEnum[]> getRoles() {
        String token = getToken();
        String url = USERS_URL + "/shops/" + shop.getShopId() + "/permissions?token=" + token;
        ResponseEntity<Map<Integer, PermissionsEnum[]>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<Integer, PermissionsEnum[]>>() {
                });

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            Notification.show("Failed to load roles: " + response.getStatusCode());
            return new HashMap<>();
        }
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
            List<DiscountDTO> discounts = getDiscounts();

            for (ItemDTO item : allItemPrices.keySet()) {
                Button addSupply, deleteItem, removeSupply, editPrice, setDiscount, removeDiscount;
                HorizontalLayout itemLayout = new HorizontalLayout();
                itemLayout.setWidthFull();
                itemLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                itemLayout.getStyle().set("border", "1px solid #ccc");
                itemLayout.getStyle().set("padding", "10px");
                itemLayout.getStyle().set("border-radius", "8px");
                itemLayout.getStyle().set("margin-bottom", "10px");

                Span itemName = new Span("Item: " + item.getName());
                Span itemPrice = new Span("Price: " + allItemPrices.get(item) + " $");
                Span itemDiscount = new Span("Discount: " +
                        (discounts.stream()
                                .filter(d -> d.getItemId() == item.getId())
                                .findFirst()
                                .map(DiscountDTO::toString)
                                .orElse("No Discount")));
                Span itemCategory = new Span("Category: " + item.getCategory());
                Span itemDescription = new Span("Description: " + item.getDescription());
                Span itemRating = new Span("Rating: " + item.getAverageRating());
                Span itemQuantity = new Span("Quantity: " + shop.getItemQuantities().getOrDefault(item.getId(), 0));
                VerticalLayout itemDetails = new VerticalLayout(itemName, itemPrice, itemDiscount, itemCategory, itemDescription, itemQuantity,
                        itemRating);
                itemDetails.setWidth("70%");

                String canManageItemsUrl = PERMISSIONS_URL + "?token=" + getToken() + "&userId="
                        + getUserId() + "&shopId=" + shop.getShopId() + "&permission="
                        + PermissionsEnum.manageItems;
                if (restTemplate.getForEntity(canManageItemsUrl, Boolean.class).getBody()) {
                    addSupply = createAddSupplyButton(item);
                    deleteItem = createDeleteItemButton(item);
                    removeSupply = createRemoveSupplyButton(item);
                    editPrice = createEditPriceButton(item);
                    setDiscount = SetDiscountButton(item);
                    removeDiscount = RemoveDiscountButton(item);
                } else {
                    addSupply = new Button("Add Supply",
                            e -> Notification.show("You do not have permission to add supply."));
                    deleteItem = new Button("Delete Item",
                            e -> Notification.show("You do not have permission to delete items."));
                    removeSupply = new Button("Remove Supply",
                            e -> Notification.show("You do not have permission to remove supply."));
                    editPrice = new Button("Edit Price",
                            e -> Notification.show("You do not have permission to edit prices."));
                    setDiscount = new Button("Set Discount",
                            e -> Notification.show("You do not have permission to set discounts."));
                    removeDiscount = new Button("Remove Discount",
                            e -> Notification.show("You do not have permission to remove discounts."));
                }

                VerticalLayout buttonsLayout = new VerticalLayout(addSupply, deleteItem, removeSupply, editPrice,
                        setDiscount, removeDiscount);
                buttonsLayout.setWidth("30%");
                itemLayout.add(itemDetails, buttonsLayout);
                itemLayout.setFlexGrow(1, itemDetails);
                itemsContainer.add(itemLayout);
            }
            //present all the discounts that are not related to items
            List<DiscountDTO> categoryDiscounts = discounts.stream()
                    .filter(d -> d.getItemCategory() != null)
                    .collect(Collectors.toList());
            List<DiscountDTO> globalDiscounts = discounts.stream()
                    .filter(d -> d.getItemCategory() == null && d.getItemId() == 0)
                    .collect(Collectors.toList());
            if (!categoryDiscounts.isEmpty() || !globalDiscounts.isEmpty()) {
                H2 discountsTitle = new H2("Discounts");
                itemsContainer.add(discountsTitle);
            }
            H2 globalDiscountsTitle = new H2("Global Discounts");
            itemsContainer.add(globalDiscountsTitle);
            for (DiscountDTO discount : globalDiscounts) {
                HorizontalLayout discountLayout = new HorizontalLayout();
                discountLayout.setWidthFull();
                discountLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                discountLayout.getStyle().set("border", "1px solid #ccc");
                discountLayout.getStyle().set("padding", "10px");
                discountLayout.getStyle().set("border-radius", "8px");
                discountLayout.getStyle().set("margin-bottom", "10px");

                Span discountInfo = new Span(discount.toString());
                // Button removeDiscountButton = createRemoveItemDiscountButton(new ItemDTO(discount.getItemId(), "", "", 0, 0, null));
                discountLayout.add(discountInfo);
                itemsContainer.add(discountLayout);
            }
            H2 categoryDiscountsTitle = new H2("Category Discounts");
            itemsContainer.add(categoryDiscountsTitle);
            for (DiscountDTO discount : categoryDiscounts) {
                HorizontalLayout discountLayout = new HorizontalLayout();
                discountLayout.setWidthFull();
                discountLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                discountLayout.getStyle().set("border", "1px solid #ccc");
                discountLayout.getStyle().set("padding", "10px");
                discountLayout.getStyle().set("border-radius", "8px");
                discountLayout.getStyle().set("margin-bottom", "10px");

                Span discountInfo = new Span(discount.toString());
                // Button removeDiscountButton = createRemoveItemDiscountButton(new ItemDTO(discount.getItemId(), "", "", 0, 0, null));
                discountLayout.add(discountInfo);
                itemsContainer.add(discountLayout);
            }
        }
    }

    private List<DiscountDTO> getDiscounts() {
        String discountsUrl = "http://localhost:8080/api/shops/" + shop.getShopId() + "/discounts?token=" + getToken();
        ResponseEntity<List<DiscountDTO>> response = restTemplate.exchange(
                discountsUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<DiscountDTO>>() {
                });
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } else {
            Notification.show("Failed to load discounts: " + response.getStatusCode());
            return new ArrayList<>();
        }
    }

    private Button RemoveDiscountButton(ItemDTO item) {
        return new Button("Remove Discount", e -> {
            Dialog dlg = new Dialog();

            // ① categories of every item in the shop
            Set<String> presentCats = shop.getItems().stream()
                .map(ItemDTO::getCategory)
                .collect(Collectors.toSet());

            boolean hasAnyItems = !shop.getItems().isEmpty();
            List<String> types = new ArrayList<>();
            if (hasAnyItems)            types.add("Global");
            if (!presentCats.isEmpty()) types.add("Category");
                                        types.add("Product");

            ComboBox<String> scopeCombo = new ComboBox<>("Discount Type", types);
            scopeCombo.setValue("Product");

            ComboBox<ItemCategory> categoryCombo = new ComboBox<>("Category");
            Set<ItemCategory> presentItemCats = presentCats.stream()
                .map(ItemCategory::valueOf)
                .collect(Collectors.toSet());
            categoryCombo.setItems(presentItemCats);
            categoryCombo.setVisible(false);

            scopeCombo.addValueChangeListener(ev ->
                categoryCombo.setVisible("Category".equals(ev.getValue()))
            );

            Button confirm = new Button("Confirm", clk -> {
                String choice = scopeCombo.getValue();
                if ("Global".equals(choice)) {
                    removeGlobalDiscount();
                }
                else if ("Category".equals(choice)) {
                    ItemCategory cat = categoryCombo.getValue();
                    if (cat == null) {
                        Notification.show("Please pick a category");
                        return;
                    }
                    removeCategoryDiscount(cat);
                }
                else {
                    removeItemDiscount(item.getId());
                }
                dlg.close();
            });

            dlg.add(new VerticalLayout(scopeCombo, categoryCombo, confirm));
            dlg.open();
        });
    }

    private void removeItemDiscount(int id) {
        String token = getToken();
        String url = SHOPS_URL + "/"
            + shop.getShopId()
            + "/discount/items/" + id
            + "?token=" + token;

        ResponseEntity<Void> resp = restTemplate.exchange(
            url, HttpMethod.DELETE, null, Void.class);

        if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
            Notification.show("Item discount removed");
            loadShopData(shop.getShopId());
            displayItems();
        } else {
            Notification.show("Failed to remove item discount");
        }
    }

    private void removeCategoryDiscount(ItemCategory category) {
        String token = getToken();
        String url = SHOPS_URL + "/"
            + shop.getShopId()
            + "/discount/categories?category=" + category.name()
            + "&token=" + token;

        ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
        if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
            Notification.show("Category discount removed");
            loadShopData(shop.getShopId());
            displayItems();
        } else {
            Notification.show("Failed to remove category discount");
        }
    }

    private void removeGlobalDiscount() {
        String token = getToken();
        String url = SHOPS_URL + "/"
            + shop.getShopId()
            + "/discount/global?token=" + token;

        ResponseEntity<Void> resp = restTemplate.exchange(
            url, HttpMethod.DELETE, null, Void.class);

        if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
            Notification.show("Global discount removed");
            loadShopData(shop.getShopId());
            displayItems();
        } else {
            Notification.show("Failed to remove global discount");
        }
    }


    private Button SetDiscountButton(ItemDTO item) {
        return new Button("Set Discount", e -> {
            Dialog dlg = new Dialog();

            Set<String> presentCats = shop.getItems().stream()
                .map(ItemDTO::getCategory)
                .collect(Collectors.toSet());

            boolean hasAnyItems = !shop.getItems().isEmpty();
            List<String> types = new ArrayList<>();
            if (hasAnyItems)            types.add("Global");
            if (!presentCats.isEmpty()) types.add("Category");
                                        types.add("Product");

            ComboBox<String> scopeCombo = new ComboBox<>("Discount Type", types);
            scopeCombo.setValue("Product");

            ComboBox<ItemCategory> categoryCombo = new ComboBox<>("Category");
            Set<ItemCategory> presentItemCats = presentCats.stream()
                .map(ItemCategory::valueOf)
                .collect(Collectors.toSet());
            categoryCombo.setItems(presentItemCats);
            categoryCombo.setVisible(false);

            NumberField pctField = new NumberField("Discount %");
            pctField.setMin(0); pctField.setMax(100);
            Checkbox dblChk = new Checkbox("Double discount", true);

            scopeCombo.addValueChangeListener(ev ->
                categoryCombo.setVisible("Category".equals(ev.getValue()))
            );

            Button confirm = new Button("Confirm", clk -> {
                String choice = scopeCombo.getValue();
                int pct = pctField.getValue().intValue();
                boolean dbl = dblChk.getValue();

                if ("Global".equals(choice)) {
                    applyGlobalDiscount(pct, dbl);
                }
                else if ("Category".equals(choice)) {
                    ItemCategory cat = categoryCombo.getValue();
                    if (cat == null) {
                        Notification.show("Please pick a category");
                        return;
                    }
                    applyCategoryDiscount(cat, pct, dbl);
                }
                else {
                    applyItemDiscount(item.getId(), pct, dbl);
                }
                dlg.close();
            });

            dlg.add(new VerticalLayout(scopeCombo, categoryCombo, pctField, dblChk, confirm));
            dlg.open();
        });
    }


    private void applyItemDiscount(int id, int pct, boolean dbl) {
        String token = getToken();
        String url = SHOPS_URL + "/"
            + shop.getShopId()
            + "/discount/items/" + id
            + "?discount=" + pct
            + "&isDouble=" + dbl
            + "&token=" + token;

        ResponseEntity<Void> resp = restTemplate.postForEntity(url, null, Void.class);
        if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
            Notification.show("Item discount applied");
            loadShopData(shop.getShopId());
            displayItems();
        } else {
            Notification.show("Failed to apply item discount");
        }
    }

    private void applyCategoryDiscount(ItemCategory category, int pct, boolean dbl) {
        String token = getToken();
        String url = SHOPS_URL + "/"
            + shop.getShopId()
            + "/discount/categories?category=" + category.name()
            + "&discount=" + pct
            + "&isDouble=" + dbl
            + "&token=" + token;

        ResponseEntity<Void> resp = restTemplate.postForEntity(url, null, Void.class);
        if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
            Notification.show("Category discount applied");
            loadShopData(shop.getShopId());
            displayItems();
        } else {
            Notification.show("Failed to apply category discount");
        }
    }

    private void applyGlobalDiscount(int pct, boolean dbl) {
        String token = getToken();
        String url = SHOPS_URL + "/"
            + shop.getShopId()
            + "/discount/global?discount=" + pct
            + "&isDouble=" + dbl
            + "&token=" + token;

        ResponseEntity<Void> resp = restTemplate.postForEntity(url, null, Void.class);
        if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
            Notification.show("Global discount applied");
            loadShopData(shop.getShopId());
            displayItems();
        } else {
            Notification.show("Failed to apply global discount");
        }
    }


    private Button createEditPriceButton(ItemDTO item) {
        return new Button("Edit Price", e -> {
            Dialog priceDialog = new Dialog();
            NumberField newPriceField = new NumberField("New Price");
            Button confirmPrice = new Button("Confirm", evt -> {
                String token = getToken();
                String url = "http://localhost:8080/api/shops/" + shop.getShopId() + "/items/"
                        + item.getId() + "/price?price=" + newPriceField.getValue().intValue()
                        + "&token=" + token;
                //print the URL for debugging
                System.out.println("Request URL: " + url);
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
    }

    private Button createAddSupplyButton(ItemDTO item) {
        return new Button("Add Supply", e -> {
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
    }

    private Button createDeleteItemButton(ItemDTO item) {
        return new Button("Delete Item", e -> {
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
    }

    private Button createRemoveSupplyButton(ItemDTO item) {
        return new Button("Remove Supply", e -> {
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
    }

    private String getToken() {
        return (String) VaadinSession.getCurrent().getAttribute("authToken");
    }

    @Override
    public void setParameter(BeforeEvent event, Integer shopId) {
    if (shopId == null || shopId <= 0) {
        Notification.show("Invalid shop ID");
        UI.getCurrent().navigate("home");
        return;
    }
    loadShopData(shopId);
    if (shop != null) {
        buildUI();
    }
}

    class UserPermissionsDTO {
        private int memberId;
        private String username;
        private PermissionsEnum[] permissions;
        private String roleName;

        public UserPermissionsDTO(int memberId, String username, PermissionsEnum[] permissions) {
            this.memberId = memberId;
            this.username = username;
            this.permissions = permissions;
            this.roleName = setRoleName(permissions);
        }

        private String setRoleName(PermissionsEnum[] permissions) {
            if (permissions == null || permissions.length == 0) {
                return "No Role";
            }
            for (PermissionsEnum permission : permissions) {
                if (permission == PermissionsEnum.closeShop)
                    return "Founder";
                if (permission == PermissionsEnum.leaveShopAsOwner)
                    return "Owner";
                if (permission == PermissionsEnum.leaveShopAsManager)
                    return "Manager";
            }
            return "No Role";
        }

        public String getUsername() {
            return username;
        }

        public PermissionsEnum[] getPermissions() {
            return permissions;
        }

        public String getRoleName() {
            return roleName;
        }

        public int getMemberId() {
            return memberId;
        }

        public String showPermissions() {
            if (permissions == null || permissions.length == 0) {
                return "No Permissions";
            }
            StringBuilder sb = new StringBuilder();
            for (PermissionsEnum permission : permissions) {
                sb.append(permission.name()).append(", ");
            }
            return sb.substring(0, sb.length() - 2); // Remove the last comma and space
        }
    }

}
