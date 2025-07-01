package UI;

import DTOs.*;
import Domain.ItemCategory;
import Domain.Operator;
import Domain.PermissionsEnum;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = "edit-shop", layout = AppLayoutBasic.class)

public class EditShopView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiBase;
    private final String usersBaseUrl;
    private final String shopsBaseUrl;
    private final String itemsBaseUrl;
    private ShopDTO shop;
    private Map<ItemDTO, Integer> allItemPrices;
    private VerticalLayout itemsContainer;
    private VerticalLayout rolesLayout;

    public EditShopView(@Value("${url.api}") String apiBase) {
        this.apiBase       = apiBase;
        this.usersBaseUrl  = apiBase + "/users";
        this.shopsBaseUrl  = apiBase + "/shops";
        this.itemsBaseUrl  = apiBase + "/items";

    }

    ////////////////////////
    private enum PredicateType {
        ITEM_QTY("Item quantity ≥ N"),
        CATEGORY_QTY("Category quantity ≥ N"),
        BASKET_VALUE("Basket total ≥ V");

        final String label;

        PredicateType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    ////////////////////////
    ///
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // Ensure userId is set in session
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }

    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    private void loadShopData(int shopId) {
        try {
            String token = getToken();
            String hasRoleUrl = usersBaseUrl + "/hasRole"
                    + "?token=" + token
                    + "&userId=" + getUserId()
                    + "&shopId=" + shopId;

            ResponseEntity<Boolean> hasRoleResponse = restTemplate.getForEntity(hasRoleUrl, Boolean.class);
            if (hasRoleResponse.getStatusCode() != HttpStatus.OK || !hasRoleResponse.getBody()) {
                Notification.show("You do not have permission to edit this shop.");
                UI.getCurrent().navigate("home");
                return;
            }

            String getShopUrl = shopsBaseUrl + "/" + shopId + "?token=" + token;
            ResponseEntity<ShopDTO> response = restTemplate.getForEntity(getShopUrl, ShopDTO.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                this.shop = response.getBody();

                String canManageUrl = usersBaseUrl + "/hasPermission?token=" + token
                        + "&userId=" + getUserId()
                        + "&shopId=" + shopId
                        + "&permission=" + PermissionsEnum.manageItems;
                boolean canManageItems = restTemplate.getForEntity(canManageUrl, Boolean.class).getBody();

                if (canManageItems) {
                    if (this.shop.getItems() == null) {
                        this.shop.setItems(new ArrayList<>());
                        this.allItemPrices = new HashMap<>();
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
                Notification.show("Failed to load shop");
            }
        } catch (Exception e) {
            Notification.show("Error loading shop");
        }
    }

    private void buildUI() {
        removeAll();

        H1 title = new H1("Edit Shop: " + shop.getName() + " 🏬✏️");
        add(title);

        // “Add Item” button
        Button addItemButton;
        String canManageItemUrl = usersBaseUrl + "/hasPermission"
                + "?token=" + getToken()
                + "&userId=" + getUserId()
                + "&shopId=" + shop.getShopId()
                + "&permission=" + PermissionsEnum.manageItems;

        boolean canManageItems = restTemplate.getForEntity(canManageItemUrl, Boolean.class).getBody();
        if (canManageItems) {
            addItemButton = new Button("Add Item", e -> openAddItemDialog());
        } else {
            addItemButton = new Button("Add Item", e -> Notification.show("You do not have permission to add items."));
        }

        // “Close Shop” and “Create New Bid” buttons
        Button closeShopButton;
        Button createAuctionButton;
        String canCloseShopUrl = usersBaseUrl + "/hasPermission"
                + "?token=" + getToken()
                + "&userId=" + getUserId()
                + "&shopId=" + shop.getShopId()
                + "&permission=" + PermissionsEnum.closeShop;
        boolean canCloseShop = restTemplate.getForEntity(canCloseShopUrl, Boolean.class).getBody();
        if (canCloseShop) {
            closeShopButton = new Button("Close Shop", e -> {
                String token = getToken();
                String url = shopsBaseUrl + "/" + shop.getShopId() + "?token=" + token;
                ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
                if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                    Notification.show("Shop closed successfully");
                    UI.getCurrent().navigate("home");
                } else {
                    Notification.show("Failed to close shop");
                }
            });
            createAuctionButton = new Button("Create Auction", e -> {
                UI.getCurrent().navigate("shop/" + shop.getShopId() + "/create-auction");
            });
            if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
                createAuctionButton.setVisible(false);
            }
        } else {
            closeShopButton = new Button("Close Shop",
                    e -> Notification.show("You do not have permission to close the shop."));
            createAuctionButton = new Button("Create Auction",
                    e -> Notification.show("You do not have permission to create auctions."));
        }

        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            addItemButton.setVisible(false);
            closeShopButton.setVisible(false);
        }
        add(addItemButton, closeShopButton, createAuctionButton);

        itemsContainer = new VerticalLayout();
        add(itemsContainer);
        displayItems();

        rolesLayout = new VerticalLayout();
        DisplayRoles();
        add(rolesLayout);
    }

    private void DisplayRoles() {
        String canManageUrl = usersBaseUrl + "/hasPermission"
            + "?token=" + getToken()
            + "&userId=" + getUserId()
            + "&shopId=" + shop.getShopId()
            + "&permission=" + PermissionsEnum.manageManagers;

        Boolean canManage = restTemplate.getForEntity(canManageUrl, Boolean.class).getBody();
        Map<Integer, PermissionsEnum[]> roles = getRoles();
        H2 rolesTitle = new H2("Roles and Permissions");
        rolesLayout.removeAll();
        rolesLayout.add(rolesTitle);

        if (roles.isEmpty()) {
            rolesLayout.add(new Span("No roles found."));
            return;
        }

        // Check permissions once
        String changePermUrl = usersBaseUrl + "/hasPermission"
                + "?token=" + getToken()
                + "&userId=" + getUserId()
                + "&shopId=" + shop.getShopId()
                + "&permission=" + PermissionsEnum.manageOwners;

        boolean canChangePermissions = restTemplate
                .getForEntity(changePermUrl, Boolean.class)
                .getBody();

        String removePermUrl = usersBaseUrl + "/hasPermission"
                + "?token=" + getToken()
                + "&userId=" + getUserId()
                + "&shopId=" + shop.getShopId()
                + "&permission=" + PermissionsEnum.manageManagers;

        boolean canRemoveMembers = restTemplate
                .getForEntity(removePermUrl, Boolean.class)
                .getBody();

        // Build list of users with roles
        List<MemberDTO> members = getShopWorkers(shop.getShopId());
        List<UserPermissionsDTO> userPermissionsList = new ArrayList<>();
        for (MemberDTO member : members) {
            PermissionsEnum[] perms = roles.get(member.getMemberId());
            if (perms != null) {
                userPermissionsList.add(
                        new UserPermissionsDTO(member.getMemberId(), member.getUsername(), perms));
            }
        }

        if (userPermissionsList.isEmpty()) {
            rolesLayout.add(new Span("No users with roles found."));
            return;
        }

        // Configure grid
        Grid<UserPermissionsDTO> rolesGrid = new Grid<>(UserPermissionsDTO.class, false);
        rolesGrid.addColumn(UserPermissionsDTO::getUsername)
                .setHeader("Username").setFlexGrow(1);
        rolesGrid.addColumn(UserPermissionsDTO::getRoleName)
                .setHeader("Role").setFlexGrow(1);
        rolesGrid.addColumn(UserPermissionsDTO::showPermissions)
                .setHeader("Permissions").setFlexGrow(2);

        // Change Permissions column (only for manageOwners)
        rolesGrid.addComponentColumn(dto -> {
            Button changeBtn = new Button("Change Permissions", e -> changePermissions(dto));
            boolean suspended = Boolean.TRUE.equals(VaadinSession.getCurrent().getAttribute("isSuspended"));
            changeBtn.setVisible(canChangePermissions && !suspended);
            return changeBtn;
        }).setHeader("Change");

        // ── Remove column ──
        rolesGrid.addComponentColumn(dto -> {
            Button removeBtn = new Button("Remove", e -> removeMemberFromShop(dto));
            boolean suspended = Boolean.TRUE.equals(VaadinSession.getCurrent().getAttribute("isSuspended"));
            boolean targetIsFounder = Arrays.stream(dto.getPermissions())
                    .anyMatch(p -> p == PermissionsEnum.manageOwners);
            removeBtn.setVisible(canRemoveMembers && !suspended && !targetIsFounder);
            return removeBtn;
        }).setHeader("Remove");

        rolesGrid.setItems(userPermissionsList);

        // Add Manager button
        Button addManager = new Button("Add Manager", e -> openAddManagerDialog(members));
        if (Boolean.TRUE.equals(VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            addManager.setVisible(false);
        }
        
        if( !canManage) {
            addManager.setVisible(false);
        }

        rolesLayout.add(addManager, rolesGrid);
    }

    // Helper to open the "Add Manager" dialog
    private void openAddManagerDialog(List<MemberDTO> members) {
        Dialog dialog = new Dialog();
        ComboBox<String> usernameField = new ComboBox<>("Username");
        List<MemberDTO> notWorking = getNotWorkingMembers(members);
        usernameField.setItems(getUserNames(notWorking));
        usernameField.setPlaceholder("Select a user");
        usernameField.setClearButtonVisible(true);

        // Exclude manageOwners from new assignments
        List<PermissionsEnum> availablePerms = Arrays.stream(PermissionsEnum.values())
                .filter(p -> p != PermissionsEnum.manageOwners &&
                        p != PermissionsEnum.suspension &&
                        p != PermissionsEnum.openClosedShop && // Exclude manageOwners
                        p != PermissionsEnum.closeShop) // Exclude closeShop
                .collect(Collectors.toList());

        CheckboxGroup<PermissionsEnum> checkboxGroup = new CheckboxGroup<>();
        checkboxGroup.setLabel("Select Permissions");
        checkboxGroup.setItems(availablePerms);

        Button confirm = new Button("Confirm", evt -> {
            String username = usernameField.getValue();
            if (username == null || username.isEmpty()) {
                Notification.show("Please select a user.");
                return;
            }
            PermissionsEnum[] selected = checkboxGroup.getValue().toArray(new PermissionsEnum[0]);
            if (selected.length == 0) {
                Notification.show("Please select at least one permission.");
                return;
            }
            int memberId = getSelectedMemberId(username, notWorking);
            String url = usersBaseUrl + "/shops/" + shop.getShopId()
                    + "/managers?memberId=" + memberId
                    + "&token=" + getToken();
            ResponseEntity<Void> resp = restTemplate.postForEntity(url, selected, Void.class);
            if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
                Notification.show(username + " got a new role in pending roles.");
                DisplayRoles();
            } else {
                Notification.show("Failed to add pending role");
            }
            dialog.close();
        });

        dialog.add(new VerticalLayout(usernameField, checkboxGroup, confirm));
        dialog.open();
    }

    private void removeMemberFromShop(UserPermissionsDTO dto) {
        Dialog dialog = new Dialog();
        Button confirmButton = new Button("Confirm", evt -> {
            String url = usersBaseUrl + "/shops/" + shop.getShopId()
                    + "/managers/" + dto.getMemberId()
                    + "?token=" + getToken();

            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                Notification.show(dto.getUsername() + " was removed from the shop.");
                DisplayRoles();
            } else {
                Notification.show("Failed to remove user from the shop");
            }
            dialog.close();
        });

        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            confirmButton.setVisible(false);
        }

        dialog.add(new VerticalLayout(new Span("Are you sure you want to remove " + dto.getUsername() + "?"),
                confirmButton));

        dialog.open();
    }

    private void changePermissions(UserPermissionsDTO dto) {
        Dialog dialog = new Dialog();
        // Build a checkbox group without 'manageOwners'
        List<PermissionsEnum> perms = Arrays.stream(PermissionsEnum.values())
                .filter(p -> p != PermissionsEnum.openClosedShop // Exclude manageOwners
                        && p != PermissionsEnum.closeShop
                        && p != PermissionsEnum.suspension) // Exclude closeShop
                .collect(Collectors.toList());
        CheckboxGroup<PermissionsEnum> checkboxGroup = new CheckboxGroup<>();
        checkboxGroup.setLabel("Select Permissions");
        checkboxGroup.setItems(perms);

        // Pre-select existing ones (minus manageOwners)
        Set<PermissionsEnum> current = Stream.of(dto.getPermissions())
                .filter(perms::contains)
                .collect(Collectors.toSet());
        checkboxGroup.setValue(current);

        Button confirmButton = new Button("Confirm", evt -> {
            Set<PermissionsEnum> selected = checkboxGroup.getValue();
            if (selected.isEmpty()) {
                Notification.show("Please select at least one permission.");
                return;
            }

            String url = usersBaseUrl + "/shops/" + shop.getShopId()
                    + "/permissions/" + dto.getMemberId()
                    + "?token=" + getToken();

            // Build JSON headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Wrap your enum array
            PermissionsEnum[] payload = selected.toArray(new PermissionsEnum[0]);
            HttpEntity<PermissionsEnum[]> request = new HttpEntity<>(payload, headers);

            try {
                // Use POST to avoid the 405
                ResponseEntity<Void> response = restTemplate.exchange(
                        url, HttpMethod.POST, request, Void.class);

                if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                    Notification.show(dto.getUsername() + "'s permissions were changed.");
                    DisplayRoles(); // refresh the grid
                } else {
                    Notification.show("Failed to change permissions: " + response.getStatusCode());
                }
            } catch (HttpClientErrorException e) {
                String body = e.getResponseBodyAsString();
                if (e.getStatusCode() == HttpStatus.CONFLICT) {
                    Notification.show("Conflict updating permissions: " + body);
                } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                    Notification.show("You don’t have permission to change roles.");
                } else {
                    Notification.show("Error changing permissions: "
                            + e.getStatusCode()
                            + " — " + body);
                }
            } finally {
                dialog.close();
            }
        });

        boolean isSuspended = Boolean.TRUE.equals(
                VaadinSession.getCurrent().getAttribute("isSuspended"));
        confirmButton.setVisible(!isSuspended);

        dialog.add(new VerticalLayout(checkboxGroup, confirmButton));
        dialog.open();
    }

    private int getSelectedMemberId(String username, List<MemberDTO> notWorkingMembers) {
        return notWorkingMembers.stream()
                .filter(member -> member.getUsername().equals(username))
                .findFirst()
                .map(MemberDTO::getMemberId)
                .orElseGet(() -> {
                    Notification.show("User not found");
                    return -1; // or throw an exception
                });
    }

    private Collection<String> getUserNames(List<MemberDTO> notWorkingMembers) {
        return notWorkingMembers.stream()
                .map(MemberDTO::getUsername)
                .collect(Collectors.toList());
    }

    private List<MemberDTO> getNotWorkingMembers(List<MemberDTO> workingMembers) {
        String url = usersBaseUrl + "/allmembers?token=" + getToken();
        ResponseEntity<List<MemberDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<MemberDTO>>() {
                });
        if (response.getStatusCode().is2xxSuccessful()) {
            List<MemberDTO> allMembers = response.getBody();
            if (allMembers == null) {
                return new ArrayList<>();
            }
            return allMembers.stream()
                    .filter(member -> workingMembers.stream()
                            .noneMatch(m -> m.getMemberId() == member.getMemberId()))
                    .collect(Collectors.toList());
        } else {
            Notification.show("Failed to load members");
            return new ArrayList<>();
        }
    }

    private List<MemberDTO> getShopWorkers(int shopId) {
        String token = getToken();
        String url = usersBaseUrl + "/shops/" + shopId + "/workers?token=" + token;
        ResponseEntity<List<MemberDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<MemberDTO>>() {
                });

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            Notification.show("Failed to load shop workers");
            return new ArrayList<>();
        }
    }

    private Map<Integer, PermissionsEnum[]> getRoles() {
        String token = getToken();
        String url = usersBaseUrl + "/shops/" + shop.getShopId() + "/permissions?token=" + token;
        ResponseEntity<Map<Integer, PermissionsEnum[]>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<Integer, PermissionsEnum[]>>() {
                });

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            Notification.show("Failed to load roles");
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
            String url = shopsBaseUrl + "/" + shop.getShopId() + "/items"
                    + "?name=" + name.getValue()
                    + "&description=" + desc.getValue()
                    + "&quantity=" + quantity.getValue().intValue()
                    + "&category=" + (category.getValue() != null ? category.getValue() : "")
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
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            confirm.setVisible(false);
        }

        dialog.add(new VerticalLayout(name, desc, price, quantity, category, confirm));
        dialog.open();
    }

    private void displayItems() {
        itemsContainer.removeAll();

        String permCheckUrl = usersBaseUrl + "/hasPermission"
                + "?token=" + getToken()
                + "&userId=" + getUserId()
                + "&shopId=" + shop.getShopId()
                + "&permission=" + PermissionsEnum.manageItems;

        boolean canManage = restTemplate.getForEntity(permCheckUrl, Boolean.class).getBody();
        if (!canManage) {
            itemsContainer.add(new Span("No permission to view items."));
            return;
        }

        if (allItemPrices == null || allItemPrices.isEmpty()) {
            itemsContainer.add(new Span("No items found."));
            return;
        }

        List<DiscountDTO> discounts = getDiscounts();

        // Display each item
        for (ItemDTO item : allItemPrices.keySet()) {
            Button addSupply, deleteItem, removeSupply, editPrice, setDiscount, removeDiscount;
            HorizontalLayout itemLayout = new HorizontalLayout();
            itemLayout.setWidthFull();
            itemLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            itemLayout.getStyle().set("border", "1px solid #ccc");
            itemLayout.getStyle().set("padding", "10px");
            itemLayout.getStyle().set("border-radius", "8px");
            itemLayout.getStyle().set("margin-bottom", "10px");

            DiscountDTO discount = discounts.stream()
                    .filter(d -> d.getItemId() != null && d.getItemId() == item.getId())
                    .findFirst()
                    .orElse(null);

            Span itemName = new Span("Item: " + item.getName());
            Span itemPrice = new Span("Price: " + allItemPrices.get(item) + " $");
            Span itemDiscount = new Span("Discount: " +
                    (discount != null ? discount.toString() : "No Discount"));
            Span itemCategory = new Span("Category: " + item.getCategory());
            Span itemDescription = new Span("Description: " + item.getDescription());
            Span itemRating = new Span("Rating: " + item.getAverageRating());
            Span itemQuantity = new Span("Quantity: " +
                    shop.getItemQuantities().getOrDefault(item.getId(), 0));

            VerticalLayout itemDetails = new VerticalLayout(
                    itemName, itemPrice, itemDiscount,
                    itemCategory, itemDescription, itemQuantity, itemRating);
            itemDetails.setWidth("70%");
            itemDetails.getStyle().set("border-right", "1px solid #ccc");

            // Buttons (Add Supply, Delete, Remove Supply, Edit Price, Set & Remove
            // Discount)
            addSupply = createAddSupplyButton(item);
            deleteItem = createDeleteItemButton(item);
            removeSupply = createRemoveSupplyButton(item);
            editPrice = createEditPriceButton(item);
            setDiscount = SetDiscountButton(item);
            removeDiscount = RemoveDiscountButton(item);

            VerticalLayout buttonsLayout = new VerticalLayout(
                    addSupply, deleteItem, removeSupply, editPrice, setDiscount, removeDiscount);
            buttonsLayout.setWidth("30%");

            itemLayout.add(itemDetails, buttonsLayout);
            itemLayout.setFlexGrow(1, itemDetails);
            itemsContainer.add(itemLayout);
        }

        // Global and Category discounts
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

        if (!globalDiscounts.isEmpty()) {
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
                discountLayout.add(discountInfo);
                itemsContainer.add(discountLayout);
            }
        }

        if (!categoryDiscounts.isEmpty()) {
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
                discountLayout.add(discountInfo);
                itemsContainer.add(discountLayout);
            }
        }
    }

    private List<DiscountDTO> getDiscounts() {
        String discountsUrl = shopsBaseUrl
                + "/" + shop.getShopId()
                + "/discounts"
                + "?token=" + getToken();

        ResponseEntity<List<DiscountDTO>> response = restTemplate.exchange(
                discountsUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<DiscountDTO>>() {
                });
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } else {
            Notification.show("Failed to load discounts");
            return new ArrayList<>();
        }
    }

    private Button RemoveDiscountButton(ItemDTO item) {
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            return new Button("Remove Discount", e -> {
                Notification.show("You are suspended and cannot remove discounts.");
            });
        }
        return new Button("Remove Discount", e -> {
            Dialog dlg = new Dialog();

            Set<String> presentCats = shop.getItems().stream()
                    .map(ItemDTO::getCategory)
                    .collect(Collectors.toSet());

            boolean hasAnyItems = !shop.getItems().isEmpty();
            List<String> types = new ArrayList<>();
            if (hasAnyItems) {
                types.add("Global");
            }
            if (!presentCats.isEmpty()) {
                types.add("Category");
            }
            types.add("Product");

            ComboBox<String> scopeCombo = new ComboBox<>("Discount Type", types);
            scopeCombo.setValue("Product");

            ComboBox<ItemCategory> categoryCombo = new ComboBox<>("Category");
            Set<ItemCategory> presentItemCats = presentCats.stream()
                    .map(ItemCategory::valueOf)
                    .collect(Collectors.toSet());
            categoryCombo.setItems(presentItemCats);
            categoryCombo.setVisible(false);

            scopeCombo.addValueChangeListener(ev -> {
                categoryCombo.setVisible("Category".equals(ev.getValue()));
            });

            Button confirm = new Button("Confirm", clk -> {
                String choice = scopeCombo.getValue();
                if ("Global".equals(choice)) {
                    removeGlobalDiscount();
                } else if ("Category".equals(choice)) {
                    ItemCategory cat = categoryCombo.getValue();
                    if (cat == null) {
                        Notification.show("Please pick a category");
                        return;
                    }
                    removeCategoryDiscount(cat);
                } else {
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
        String url = shopsBaseUrl
                + "/" + shop.getShopId()
                + "/discount/items/" + id
                + "?token=" + token;

        ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
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
        String url = shopsBaseUrl
                + "/" + shop.getShopId()
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
        String url = shopsBaseUrl
                + "/" + shop.getShopId()
                + "/discount/global?token=" + token;

        ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class);
        if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
            Notification.show("Global discount removed");
            loadShopData(shop.getShopId());
            displayItems();
        } else {
            Notification.show("Failed to remove global discount");
        }
    }

    private Button SetDiscountButton(ItemDTO item) {
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            return new Button("Set Discount", e -> {
                Notification.show("You are suspended and cannot set discounts.");
            });
        }
        return new Button("Set Discount", e -> {
            Dialog dlg = new Dialog();

            Set<String> presentCats = shop.getItems().stream()
                    .map(ItemDTO::getCategory)
                    .collect(Collectors.toSet());

            boolean hasAnyItems = !shop.getItems().isEmpty();
            List<String> types = new ArrayList<>();
            if (hasAnyItems) {
                types.add("Global");
            }
            if (!presentCats.isEmpty()) {
                types.add("Category");
            }
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
            pctField.setMin(0);
            pctField.setMax(100);
            Checkbox dblChk = new Checkbox("Double discount", true);

            scopeCombo.addValueChangeListener(ev -> {
                categoryCombo.setVisible("Category".equals(ev.getValue()));
            });

            Button confirm = new Button("Confirm", clk -> {
                String choice = scopeCombo.getValue();
                int pct = pctField.getValue().intValue();
                boolean dbl = dblChk.getValue();
                ItemCategory chosenCategory = categoryCombo.getValue();

                if ("Category".equals(choice) && chosenCategory == null) {
                    Notification.show("Please pick a category");
                    return;
                }

                dlg.close();

                Dialog askPolicy = new Dialog();
                askPolicy.add(new H1("Add a policy for this discount?"));

                Button yes = new Button("Yes", ev -> {
                    askPolicy.close();
                    Integer contextId = "Product".equals(choice) ? item.getId() : null;
                    ItemCategory ctxCat = "Category".equals(choice) ? chosenCategory : null;
                    openPolicyDialog(
                            new ArrayList<>(),
                            new ArrayList<>(),
                            choice,
                            contextId,
                            ctxCat,
                            policy -> {
                                shopPolicyApi(shop.getShopId(), policy);
                                applyDiscount(choice, item.getId(), chosenCategory, pct, dbl);
                                Notification.show("Policy added and discount applied");
                                loadShopData(shop.getShopId());
                                displayItems();
                            });
                });

                Button no = new Button("No", ev -> {
                    askPolicy.close();
                    applyDiscount(choice, item.getId(), chosenCategory, pct, dbl);
                    Notification.show("Discount applied");
                });

                askPolicy.add(new HorizontalLayout(yes, no));
                askPolicy.open();
            });

            dlg.add(new VerticalLayout(scopeCombo, categoryCombo, pctField, dblChk, confirm));
            dlg.open();
        });
    }

    // Utility to route to the appropriate discount call
    private void applyDiscount(String scope, int itemId, ItemCategory cat, int pct, boolean dbl) {
        if ("Global".equals(scope)) {
            applyGlobalDiscount(pct, dbl);
        } else if ("Category".equals(scope)) {
            applyCategoryDiscount(cat, pct, dbl);
        } else {
            applyItemDiscount(itemId, pct, dbl);
        }
    }

    // Recursive helper to build a CompositePolicyDTO
    private void openPolicyDialog(
            List<LeafPolicyDTO> leaves,
            List<Operator> ops,
            String scope,
            Integer contextItemId,
            ItemCategory contextCategory,
            Consumer<CompositePolicyDTO> onSave) {
        Dialog dlg = new Dialog();
        dlg.setWidth("400px");

        // 1️⃣ Template chooser
        ComboBox<PredicateType> typeBox = new ComboBox<>("Predicate type", List.of(PredicateType.values()));
        if ("Product".equals(scope)) {
            typeBox.setItems(PredicateType.ITEM_QTY, PredicateType.BASKET_VALUE);
        } else if ("Category".equals(scope)) {
            typeBox.setItems(PredicateType.CATEGORY_QTY, PredicateType.BASKET_VALUE);
        } else { // Global
            typeBox.setItems(PredicateType.BASKET_VALUE);
        }
        typeBox.setItemLabelGenerator(PredicateType::toString);

        // 2️⃣ Dynamic fields
        NumberField itemIdField = new NumberField("Item Id");
        NumberField itemQtyField = new NumberField("Min quantity");
        ComboBox<ItemCategory> catBox = new ComboBox<>("Category", Arrays.asList(ItemCategory.values()));
        NumberField catQtyField = new NumberField("Min quantity");
        NumberField basketValField = new NumberField("Min basket value");

        // ─── PRE-FILL ITEM ID if contextItemId != null
        if (contextItemId != null) {
            itemIdField.setValue(contextItemId.doubleValue());
            itemIdField.setReadOnly(true);
            itemIdField.setVisible(true);
        } else {
            itemIdField.setVisible(false);
        }

        // ─── PRE-FILL CATEGORY if contextCategory != null
        if (contextCategory != null) {
            catBox.setValue(contextCategory);
            catBox.setReadOnly(true);
            catBox.setVisible(true);
        } else {
            catBox.setVisible(false);
        }

        itemQtyField.setVisible(false);
        catQtyField.setVisible(false);
        basketValField.setVisible(false);

        typeBox.addValueChangeListener(evt -> {
            PredicateType t = evt.getValue();
            itemIdField.setVisible(false);
            itemQtyField.setVisible(false);
            catBox.setVisible(false);
            catQtyField.setVisible(false);
            basketValField.setVisible(false);

            switch (t) {
                case ITEM_QTY:
                    if (contextItemId == null) {
                        itemIdField.setVisible(true);
                    }
                    itemQtyField.setVisible(true);
                    break;
                case CATEGORY_QTY:
                    if (contextCategory == null) {
                        catBox.setVisible(true);
                    }
                    catQtyField.setVisible(true);
                    break;
                case BASKET_VALUE:
                    basketValField.setVisible(true);
                    break;
            }
        });

        Button save = new Button("Save", ev -> {
            PredicateType t = typeBox.getValue();
            if (t == null) {
                Notification.show("Please choose a predicate type");
                return;
            }

            LeafPolicyDTO leaf;
            switch (t) {
                case ITEM_QTY:
                    if (itemQtyField.isEmpty() || itemQtyField.getValue() == null
                            || itemQtyField.getValue().intValue() <= 0) {
                        Notification.show("Please enter a minimum quantity");
                        return;
                    }
                    int q = itemQtyField.getValue().intValue();
                    Integer iid = (contextItemId != null) ? contextItemId : itemIdField.getValue().intValue();
                    leaf = new LeafPolicyDTO(q, iid, null, 0.0);
                    break;

                case CATEGORY_QTY:
                    if (catQtyField.isEmpty() || catQtyField.getValue() == null) {
                        Notification.show("Please enter a minimum quantity for the category");
                        return;
                    }
                    int qc = catQtyField.getValue().intValue();
                    ItemCategory chosenCat = (contextCategory != null) ? contextCategory : catBox.getValue();
                    if (chosenCat == null) {
                        Notification.show("Please pick a category");
                        return;
                    }
                    leaf = new LeafPolicyDTO(qc, null, chosenCat, 0.0);
                    break;

                case BASKET_VALUE:
                    if (basketValField.isEmpty() || basketValField.getValue() == null) {
                        Notification.show("Please enter a basket value");
                        return;
                    }
                    double v = basketValField.getValue();
                    leaf = new LeafPolicyDTO(null, null, null, v);
                    break;

                default:
                    Notification.show("Unexpected predicate type: " + t);
                    return;
            }

            leaves.add(leaf);
            dlg.close();

            Dialog another = new Dialog();
            another.add(new H1("Add another predicate?"));
            Button yes = new Button("Yes", e -> {
                another.close();
                if (!leaves.isEmpty()) {
                    openOperatorDialog(leaves, ops, scope, contextItemId, contextCategory, onSave);
                } else {
                    openPolicyDialog(leaves, ops, scope, contextItemId, contextCategory, onSave);
                }
            });
            Button no = new Button("No", e -> {
                another.close();
                CompositePolicyDTO comp = buildComposite(leaves, ops);
                onSave.accept(comp);
            });
            another.add(new HorizontalLayout(yes, no));
            another.open();
        });

        dlg.add(new VerticalLayout(
                typeBox,
                itemIdField, itemQtyField,
                catBox, catQtyField,
                basketValField,
                save));
        dlg.open();
    }

    private CompositePolicyDTO buildComposite(List<LeafPolicyDTO> leaves, List<Operator> ops) {
        if (leaves.isEmpty()) {
            Notification.show("No predicates provided");
        }
        if (leaves.size() == 1) {
            return new CompositePolicyDTO(
                    null, null,
                    leaves.get(0), null,
                    null);
        }
        CompositePolicyDTO current = new CompositePolicyDTO(
                null, null,
                leaves.get(0), leaves.get(1),
                ops.get(0));
        for (int i = 2; i < leaves.size(); i++) {
            current = new CompositePolicyDTO(
                    current, null,
                    null, leaves.get(i),
                    ops.get(i - 1));
        }
        return current;
    }

    private void shopPolicyApi(int shopId, CompositePolicyDTO dto) {
        String url = shopsBaseUrl
                + "/" + shopId
                + "/discount/policy?token=" + getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CompositePolicyDTO> requestEntity = new HttpEntity<>(dto, headers);
        restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
    }

    private void openOperatorDialog(
            List<LeafPolicyDTO> leaves,
            List<Operator> ops,
            String scope,
            Integer contextItemId,
            ItemCategory contextCategory,
            Consumer<CompositePolicyDTO> onSave) {
        Dialog opDlg = new Dialog();
        RadioButtonGroup<Operator> rg = new RadioButtonGroup<>("Operator");
        rg.setItems(Operator.values());
        rg.setItemLabelGenerator(Operator::name);
        rg.setLabel("Combine with previous rule");

        Button ok = new Button("OK", ev -> {
            ops.add(rg.getValue());
            opDlg.close();
            openPolicyDialog(leaves, ops, scope, contextItemId, contextCategory, onSave);
        });
        opDlg.add(new VerticalLayout(rg, ok));
        opDlg.open();
    }

    private void applyItemDiscount(int id, int pct, boolean dbl) {
        String token = getToken();
        String url = shopsBaseUrl
                + "/" + shop.getShopId()
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
        String url = shopsBaseUrl
                + "/" + shop.getShopId()
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
        String url = shopsBaseUrl
                + "/" + shop.getShopId()
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
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            return new Button("Edit Price", e -> {
                Notification.show("You are suspended and cannot edit prices.");
            });
        }
        return new Button("Edit Price", e -> {
            Dialog priceDialog = new Dialog();
            NumberField newPriceField = new NumberField("New Price");
            Button confirmPrice = new Button("Confirm", evt -> {
                String token = getToken();
                String url = shopsBaseUrl
                        + "/" + shop.getShopId()
                        + "/items/" + item.getId()
                        + "/price?price=" + newPriceField.getValue().intValue()
                        + "&token=" + token;

                ResponseEntity<Void> priceResponse = restTemplate.postForEntity(url, null, Void.class);
                if (priceResponse.getStatusCode().is2xxSuccessful()) {
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
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            return new Button("Add Supply", e -> {
                Notification.show("You are suspended and cannot add supply.");
            });
        }

        return new Button("Add Supply", e -> {
            Dialog supplyDialog = new Dialog();
            TextField supplyQuantity = new TextField("Supply Quantity");
            Button confirmSupply = new Button("Confirm", evt -> {
                String token = getToken();
                String url = shopsBaseUrl
                        + "/" + shop.getShopId()
                        + "/items/" + item.getId()
                        + "/supply?quantity=" + supplyQuantity.getValue()
                        + "&token=" + token;

                ResponseEntity<Void> supplyResponse = restTemplate.postForEntity(url, null, Void.class);
                if (supplyResponse.getStatusCode().is2xxSuccessful()) {
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
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            return new Button("Delete Item", e -> {
                Notification.show("You are suspended and cannot delete items.");
            });
        }
        return new Button("Delete Item", e -> {
            String token = getToken();

            String url = shopsBaseUrl
                    + "/" + shop.getShopId()
                    + "/items/" + item.getId()
                    + "?token=" + token;

             // send the token in the Authorization header
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Void> deleteResponse =
                restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);

            if (deleteResponse.getStatusCode() == HttpStatus.NO_CONTENT) {
                Notification.show("Item deleted successfully");
                loadShopData(shop.getShopId());
                displayItems();
            } else {
                Notification.show("Failed to delete item: " + deleteResponse.getStatusCode());
            }
        });
    }

    private Button createRemoveSupplyButton(ItemDTO item) {
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            return new Button("Remove Supply", e -> {
                Notification.show("You are suspended and cannot remove supply.");
            });
        }
        return new Button("Remove Supply", e -> {
            Dialog removeSupplyDialog = new Dialog();
            TextField removeQuantity = new TextField("Remove Quantity");
            Button confirmRemove = new Button("Confirm", evt -> {
                String token = getToken();
                String url = shopsBaseUrl
                        + "/" + shop.getShopId()
                        + "/items/" + item.getId()
                        + "/supply/remove?supply=" + removeQuantity.getValue()
                        + "&token=" + token;

                ResponseEntity<Void> removeResponse = restTemplate.postForEntity(url, null, Void.class);
                if (removeResponse.getStatusCode().is2xxSuccessful()) {
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
        try {
            if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
                event.forwardTo("login");
                return;
            }

            if (shopId == null || shopId <= 0) {
                Notification.show("Invalid shop ID");
                UI.getCurrent().navigate("home");
                return;
            }
            loadShopData(shopId);
            if (shop != null) {
                buildUI();
            }
        } catch (Exception e) {
            Notification.show("Error loading shop data");
            UI.getCurrent().navigate("home");
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
            String answer = "No Role";
            for (PermissionsEnum permission : permissions) {
                if (permission == PermissionsEnum.manageOwners || permission == PermissionsEnum.closeShop)
                    answer = "Founder";
                if (permission == PermissionsEnum.leaveShopAsOwner
                        && (answer.equals("No Role") || answer.equals("Manager")))
                    answer = "Owner";
                if (permission == PermissionsEnum.leaveShopAsManager && answer.equals("No Role"))
                    answer = "Manager";
            }
            return answer;
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
            return sb.substring(0, sb.length() - 2);
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
        String url = usersBaseUrl
                + "/" + userId
                + "/isSuspended?token=" + token;

        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }

}
