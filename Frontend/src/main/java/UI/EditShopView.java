package UI;

import DTOs.DiscountDTO;
import DTOs.ItemDTO;
import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;
import DTOs.policiesDTO;
import DTOs.rolesDTO;
import main.java.Domain.ItemCategory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.*;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;

import main.java.Domain.PermissionsEnum;

@Route(value = "editShop", layout = AppLayoutBasic.class)
public class EditShopView extends VerticalLayout implements HasUrlParameter<String> {

    private ShopDTO shop;
    private VerticalLayout itemsContainer;
    private VerticalLayout rolesContainer;
    private VerticalLayout discountVerticalLayout;

    private ComboBox<ItemCategory> categoryFilter;
    private NumberField minPriceField;
    private NumberField maxPriceField;
    private TextField nameSearchField;

    private List<ItemDTO> allItems;
    private List<rolesDTO> allRoles; // Store the full list of roles
    private List<DiscountDTO> allDiscounts; // Store the full list of discounts

    public EditShopView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setSpacing(true);
        setPadding(true);
    }

    @Override
    public void setParameter(BeforeEvent event, String shopName) {
        // Replace spaces with dashes in shop name

        loadShopData(shopName);

        removeAll(); // Clear previous content

        H1 title = new H1("🛍️ Admin View – " + shop.getName());
        add(title);

        Button historyButton = new Button("📝 View History", e -> {
            getUI().ifPresent(ui -> ui.navigate("history/" + shop.getName()));
        });
        Span avgRating = new Span("⭐ Average Rating: " + calculateAverageRating(shop.getReviews()));
        avgRating.getStyle().set("align-self", "center");
        HorizontalLayout ratingLayout = new HorizontalLayout(historyButton, avgRating);
        Button viewReviewsButton = new Button("📋 View Reviews", e -> {
            Dialog reviewsDialog = new Dialog();
            reviewsDialog.setHeaderTitle("Reviews for " + shop.getName());

            VerticalLayout reviewsContent = new VerticalLayout();
            for (ShopReviewDTO review : shop.getReviews()) {
                reviewsContent.add(new Span("⭐ " + review.getRating() + " - " + review.getReviewText()));
            }

            Button closeButton = new Button("Close", ev -> reviewsDialog.close());
            reviewsContent.add(closeButton);

            reviewsDialog.add(reviewsContent);
            reviewsDialog.open();
        });
        ratingLayout.add(viewReviewsButton);
        ratingLayout.setWidthFull();
        ratingLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        add(ratingLayout);

        allItems = new ArrayList<>(shop.getItems().keySet());

        HorizontalLayout discountButtons = new HorizontalLayout();
        discountButtons.setSpacing(true);

        Button globalDiscountButton = new Button("➕ Set Global Discount", e -> openGlobalDiscountDialog());
        Button itemDiscountButton = new Button("➕ Set Item Discount", e -> openItemDiscountDialog());
        Button categoryDiscountButton = new Button("➕ Set Category Discount", e -> openCategoryDiscountDialog());

        discountButtons.add(globalDiscountButton, itemDiscountButton, categoryDiscountButton);

        add(discountButtons);

        itemsContainer = new VerticalLayout();
        itemsContainer.setHeight("70vh");
        itemsContainer.getStyle().set("overflow", "auto");

        rolesContainer = new VerticalLayout();
        rolesContainer.setHeight("70vh");
        rolesContainer.getStyle().set("overflow", "auto");

        discountVerticalLayout = new VerticalLayout();
        discountVerticalLayout.setHeight("70vh");
        discountVerticalLayout.getStyle().set("overflow", "auto");

        // Create buttons and layouts for items
        Button addItemButton = new Button("➕ Add Item", e -> {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Add New Item");

            TextField nameField = new TextField("Name");
            TextField descField = new TextField("Description");
            NumberField priceField = new NumberField("Price");
            NumberField quantityField = new NumberField("Quantity");
            ComboBox<ItemCategory> categoryField = new ComboBox<>("Category");
            categoryField.setItems(ItemCategory.values());

            Button confirmButton = new Button("Confirm", ev -> {
                if (nameField.isEmpty() || descField.isEmpty() || priceField.isEmpty() || quantityField.isEmpty()
                        || categoryField.isEmpty()) {
                    Notification.show("Please fill all fields");
                    return;
                }
                ItemDTO newItem = new ItemDTO(allItems.size() + 1, nameField.getValue(),
                        descField.getValue(), priceField.getValue(), categoryField.getValue());
                allItems.add(newItem);
                shop.getItems().put(newItem, quantityField.getValue().intValue());
                shop.getPrices().put(newItem, priceField.getValue().intValue());
                displayShopItems(allItems);
                dialog.close();
            });

            Button cancelButton = new Button("Cancel", ev -> dialog.close());
            dialog.add(new VerticalLayout(nameField, descField, priceField, quantityField,
                    categoryField, new HorizontalLayout(confirmButton, cancelButton)));
            dialog.open();
        });

        VerticalLayout itemSection = new VerticalLayout(addItemButton, itemsContainer);
        itemSection.setWidthFull();
        itemSection.setHeight("70vh");
        itemSection.getStyle().set("overflow", "auto");
        itemSection.setAlignItems(Alignment.CENTER);

        // Create buttons and layouts for roles
        Button addRoleButton = new Button("➕ Add Role", e -> {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Add New Role");

            TextField roleNameField = new TextField("Role Name");
            TextField userNameField = new TextField("Username");
            Checkbox isOwnerField = new Checkbox("Is Owner?");
            isOwnerField.setValue(false);

            Button confirmButton = new Button("Confirm", ev -> {
                if (roleNameField.isEmpty() || userNameField.isEmpty()) {
                    Notification.show("Please fill all fields");
                    return;
                }
                rolesDTO newRole = new rolesDTO(roleNameField.getValue(), new ArrayList<>(),
                        shop.getName(), userNameField.getValue());
                allRoles.add(newRole);
                displayShopRoles(allRoles);
                dialog.close();
            });

            Button cancelButton = new Button("Cancel", ev -> dialog.close());
            dialog.add(new VerticalLayout(roleNameField, userNameField, isOwnerField,
                    new HorizontalLayout(confirmButton, cancelButton)));
            dialog.open();
        });

        VerticalLayout roleSection = new VerticalLayout(addRoleButton, rolesContainer);
        roleSection.setWidthFull();
        roleSection.setHeight("70vh");
        roleSection.getStyle().set("overflow", "auto");
        roleSection.setAlignItems(Alignment.CENTER);

        VerticalLayout DiscountSection = new VerticalLayout(discountButtons, discountVerticalLayout);
        DiscountSection.setWidthFull();
        DiscountSection.setHeight("70vh");
        DiscountSection.getStyle().set("overflow", "auto");
        DiscountSection.setAlignItems(Alignment.CENTER);

        HorizontalLayout content = new HorizontalLayout(setupFilters(), itemSection, roleSection, DiscountSection);
        content.setWidthFull();
        add(content);

        displayShopItems(allItems);
        displayShopRoles(allRoles);
        displayShopDiscounts(allDiscounts);

        Button removeShopBtn = new Button("❌ Delete Shop", e -> {
            // TODO: Replace with actual service call
            Notification.show("Shop '" + shop.getName() + "' removed (mock)");
            getUI().ifPresent(ui -> ui.navigate("shops")); // redirect to shop list
        });
        removeShopBtn.getStyle()
                .set("color", "white")
                .set("background-color", "#e74c3c")
                .set("margin-bottom", "10px");

        add(removeShopBtn);

        Dialog confirmDialog = new Dialog();
        confirmDialog.add("Are you sure you want to delete the shop?");
        Button confirm = new Button("Yes", evt -> {
            confirmDialog.close();
            // call delete shop service
        });
        Button cancel = new Button("Cancel", evt -> confirmDialog.close());
        confirmDialog.add(new HorizontalLayout(confirm, cancel));

        removeShopBtn.addClickListener(e -> confirmDialog.open());
    }

    private void openGlobalDiscountDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new H3("Set Global Discount"));

        NumberField percentageField = new NumberField("Discount %");
        percentageField.setMin(0);
        percentageField.setMax(100);
        percentageField.setSuffixComponent(new Span("%"));

        Button applyButton = new Button("Apply", e -> {
            if (percentageField.getValue() == null) {
                Notification.show("Enter a valid discount");
                return;
            }
            // TODO: call backend to apply global discount
            Notification.show("Global discount set: " + percentageField.getValue() + "%");
            dialog.close();
        });
        Button policyButton = new Button("set policy", e -> {
            Dialog dialog2 = openPolicyDialog();
            dialog2.open();
        });
        layout.add(percentageField, policyButton, applyButton);
        dialog.add(layout);
        dialog.open();
    }

    private void openItemDiscountDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new H3("Set Discount for Item"));

        ComboBox<ItemDTO> itemSelect = new ComboBox<>("Select Item");
        itemSelect.setItems(allItems);
        itemSelect.setItemLabelGenerator(ItemDTO::getName);

        NumberField percentageField = new NumberField("Discount %");
        percentageField.setMin(0);
        percentageField.setMax(100);
        percentageField.setSuffixComponent(new Span("%"));

        Button applyButton = new Button("Apply", e -> {
            if (itemSelect.getValue() == null || percentageField.getValue() == null) {
                Notification.show("Select an item and enter a discount");
                return;
            }
            // TODO: call backend to apply discount to item
            Notification
                    .show("Discount for " + itemSelect.getValue().getName() + ": " + percentageField.getValue() + "%");
            dialog.close();
        });
        Button policyButton = new Button("set policy", e -> {
            Dialog dialog2 = openPolicyDialog();
            dialog2.open();
        });
        layout.add(itemSelect, percentageField, policyButton, applyButton);
        dialog.add(layout);
        dialog.open();
    }

    private void openCategoryDiscountDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new H3("Set Discount for Category"));

        ComboBox<ItemCategory> categorySelect = new ComboBox<>("Select Category");
        categorySelect.setItems(ItemCategory.values());

        NumberField percentageField = new NumberField("Discount %");
        percentageField.setMin(0);
        percentageField.setMax(100);
        percentageField.setSuffixComponent(new Span("%"));

        Button applyButton = new Button("Apply", e -> {
            if (categorySelect.getValue() == null || percentageField.getValue() == null) {
                Notification.show("Select a category and enter a discount");
                return;
            }
            // TODO: call backend to apply discount to category
            Notification.show("Discount for " + categorySelect.getValue() + ": " + percentageField.getValue() + "%");
            dialog.close();
        });
        Button policyButton = new Button("set policy", e -> {
            Dialog dialog2 = openPolicyDialog();
            dialog2.open();
        });
        layout.add(categorySelect, percentageField, policyButton, applyButton);
        dialog.add(layout);
        dialog.open();
    }

    private Dialog openPolicyDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new H3("Set Policy"));
        layout.add(new H4(
                "please enter threshold and category, threshold and item name or basket value. if all are not entered, assume there is no policy"));
        NumberField thresholdField = new NumberField("Threshold");
        TextField itemNameField = new TextField("Item Name");
        ComboBox<ItemCategory> categorySelect = new ComboBox<>("Select Category");
        categorySelect.setItems(ItemCategory.values());
        NumberField basketValueField = new NumberField("Basket Value");

        Button applyButton = new Button("Apply", e -> {
            if (!thresholdField.isEmpty() && !itemNameField.isEmpty()) {
                Notification.show("item threshold was selected");
                return;
            } else if (!thresholdField.isEmpty() && !categorySelect.isEmpty()) {
                Notification.show("category threshold was selected");
                return;
            } else if (!basketValueField.isEmpty()) {
                Notification.show("basket value was selected");
                return;
            } else if (thresholdField.isEmpty() && itemNameField.isEmpty()
                    && categorySelect.isEmpty() && basketValueField.isEmpty()) {
                Notification.show("everyone get this discount");
                return;
            } else {
                Notification.show("please enter a valid policy");
                return;
            }
        });
        layout.add(thresholdField, itemNameField, categorySelect, basketValueField, applyButton);
        dialog.add(layout);
        return dialog;
    }

    public void loadShopData(String shopName) {
        // Simulate loading shop data from a service or database
        // In a real application, you would replace this with actual data retrieval
        // logic
        Map<ItemDTO, Integer> items = new HashMap<>();
        Map<ItemDTO, Integer> prices = new HashMap<>();
        List<ShopReviewDTO> reviews = new ArrayList<>();
        // Mock data for demonstration purposes
        ItemDTO item1 = new ItemDTO(1, "Item 1", "Description 1", 10.0, ItemCategory.ELECTRONICS);
        ItemDTO item2 = new ItemDTO(2, "Item 2", "Description 2", 20.0, ItemCategory.CLOTHING);
        ItemDTO item3 = new ItemDTO(3, "Item 3", "Description 3", 30.0, ItemCategory.CLOTHING);
        items.put(item1, 10);
        items.put(item2, 5);
        items.put(item3, 15);
        prices.put(item1, 10);
        prices.put(item2, 20);
        prices.put(item3, 30);

        reviews.add(new ShopReviewDTO(1, 5, "Great shop!", shopName));
        reviews.add(new ShopReviewDTO(2, 4, "Good service.", shopName));
        reviews.add(new ShopReviewDTO(3, 3, "Average experience.", shopName));
        reviews.add(new ShopReviewDTO(4, 2, "Not satisfied.", shopName));

        shop = new ShopDTO(shopName, items, prices, reviews);
        allItems = new ArrayList<>(shop.getItems().keySet());

        allRoles = new ArrayList<>();
        allRoles.add(new rolesDTO("Admin", List.of("ADD_ITEM", "REMOVE_ITEM"), shopName, "user1"));
        allRoles.add(new rolesDTO("Manager", List.of("VIEW_ITEM", "EDIT_ITEM"), shopName, "user2"));
        allRoles.add(new rolesDTO("Staff", List.of("VIEW_ITEM"), shopName, "user3"));

        allDiscounts = new ArrayList<>();
        allDiscounts.add(new DiscountDTO(5, false, ItemCategory.ELECTRONICS, null));
        allDiscounts.add(new DiscountDTO(10, true, ItemCategory.AUTOMOTIVE, "Item 2"));
        allDiscounts.add(new DiscountDTO(15, false, null, "Item 3"));
    }

    private double calculateAverageRating(List<ShopReviewDTO> reviews) {
        if (reviews.isEmpty()) {
            return 0.0;
        }
        double totalRating = 0.0;
        for (ShopReviewDTO review : reviews) {
            totalRating += review.getRating();
        }
        return totalRating / reviews.size();
    }

    private VerticalLayout setupFilters() {
        VerticalLayout filterLayout = new VerticalLayout();
        filterLayout.setWidth("15%");
        filterLayout.setPadding(true);
        filterLayout.setSpacing(true);

        categoryFilter = new ComboBox<>("Category");
        categoryFilter.setItems(ItemCategory.values());
        categoryFilter.addValueChangeListener(e -> applyFilters());

        minPriceField = new NumberField("Min Price");
        minPriceField.setPlaceholder("Min Price");
        minPriceField.addValueChangeListener(e -> applyFilters());

        maxPriceField = new NumberField("Max Price");
        maxPriceField.setPlaceholder("Max Price");
        maxPriceField.addValueChangeListener(e -> applyFilters());

        nameSearchField = new TextField("Search by Name");
        nameSearchField.setPlaceholder("Item Name");
        nameSearchField.addValueChangeListener(e -> applyFilters());

        Button clearFiltersButton = new Button("Clear Filters", e -> clearFilters());

        filterLayout.add(categoryFilter, minPriceField, maxPriceField, nameSearchField, clearFiltersButton);

        return filterLayout;
    }

    private void applyFilters() {
        List<ItemDTO> filteredItems = new ArrayList<>(allItems);

        if (categoryFilter.getValue() != null) {
            filteredItems.removeIf(item -> !item.getCategory().equals(categoryFilter.getValue()));
        }

        if (minPriceField.getValue() != null) {
            filteredItems.removeIf(item -> item.getPrice() < minPriceField.getValue());
        }

        if (maxPriceField.getValue() != null) {
            filteredItems.removeIf(item -> item.getPrice() > maxPriceField.getValue());
        }

        if (!nameSearchField.isEmpty()) {
            String searchTerm = nameSearchField.getValue().toLowerCase();
            filteredItems.removeIf(item -> !item.getName().toLowerCase().contains(searchTerm));
        }

        displayShopItems(filteredItems);
    }

    private void clearFilters() {
        categoryFilter.clear();
        minPriceField.clear();
        maxPriceField.clear();
        nameSearchField.clear();
        displayShopItems(allItems);
    }

    private void displayShopItems(List<ItemDTO> items) {
        itemsContainer.removeAll();

        if (items.isEmpty()) {
            Span noItems = new Span("No items available for current filters.");
            noItems.getStyle().set("color", "red").set("font-size", "18px").set("font-weight", "bold");
            itemsContainer.add(noItems);
            return;
        }

        for (ItemDTO item : items) {
            VerticalLayout itemCard = new VerticalLayout();
            itemCard.setWidth("100%");
            itemCard.getStyle()
                    .set("border", "1px solid #ccc")
                    .set("border-radius", "8px")
                    .set("padding", "10px")
                    .set("box-shadow", "0 2px 5px rgba(0,0,0,0.05)")
                    .set("margin-bottom", "10px")
                    .set("background-color", "#f9f9f9");

            TextField nameField = new TextField("Name", item.getName());
            nameField.setValue(item.getName());
            nameField.setReadOnly(true);
            TextField descField = new TextField("Description", item.getDescription());
            descField.setValue(item.getDescription());
            descField.setReadOnly(true);
            TextField categoryField = new TextField("Category", String.valueOf(item.getCategory()));
            categoryField.setValue(String.valueOf(item.getCategory()));
            categoryField.setReadOnly(true);
            NumberField priceField = new NumberField("Price");
            priceField.setValue(shop.getPrices().getOrDefault(item, 0).doubleValue());
            priceField.setReadOnly(true);
            NumberField quantityField = new NumberField("Quantity");
            quantityField.setValue(shop.getPrices().getOrDefault(item, 0).doubleValue());
            quantityField.setReadOnly(true);

            Button addQuantityButton = new Button("📦 Add Stock", e -> {
                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Add Stock for " + item.getName());

                NumberField unitsField = new NumberField("Units to Add");
                unitsField.setMin(1);
                unitsField.setValue(1.0);

                Button confirmButton = new Button("Confirm", ev -> {
                    if (unitsField.getValue() == null || unitsField.getValue() < 1) {
                        Notification.show("Please enter a valid number of units");
                        return;
                    }
                    // TODO: Add call to backend service to update quantity
                    int currentQuantity = shop.getItems().getOrDefault(item, 0);
                    shop.getItems().put(item, currentQuantity + unitsField.getValue().intValue());
                    Notification.show("Added " + unitsField.getValue().intValue() + " units");
                    dialog.close();
                    displayShopItems(allItems);
                });

                Button cancelButton = new Button("Cancel", ev -> dialog.close());

                dialog.add(
                        new VerticalLayout(
                                unitsField,
                                new HorizontalLayout(confirmButton, cancelButton)));

                dialog.open();
            });

            Button removeButton = new Button("❌ Remove", e -> {
                shop.getItems().remove(item);
                shop.getPrices().remove(item);
                allItems.remove(item);
                displayShopItems(allItems);
            });

            itemCard.add(nameField, descField, categoryField, priceField, quantityField,
                    new HorizontalLayout(addQuantityButton, removeButton));
            itemsContainer.add(itemCard);
        }
    }

    private void displayShopRoles(List<rolesDTO> roles) {
        rolesContainer.removeAll();

        if (roles.isEmpty()) {
            Span noRoles = new Span("No roles available for current filters.");
            noRoles.getStyle().set("color", "red").set("font-size", "18px").set("font-weight", "bold");
            rolesContainer.add(noRoles);
            return;
        }

        for (rolesDTO role : roles) {
            VerticalLayout roleCard = new VerticalLayout();
            roleCard.setWidth("100%");
            roleCard.getStyle()
                    .set("border", "1px solid #ccc")
                    .set("border-radius", "8px")
                    .set("padding", "10px")
                    .set("box-shadow", "0 2px 5px rgba(0,0,0,0.05)")
                    .set("margin-bottom", "10px")
                    .set("background-color", "#f9f9f9");

            TextField nameField = new TextField("Role Name", role.getRoleName());
            nameField.setValue(role.getRoleName());
            nameField.setReadOnly(true);
            Span permissionsField = new Span("Permissions: " + String.join(", ", role.getPermissions()));
            permissionsField.getStyle()
                    .set("margin", "10px 0")
                    .set("display", "block")
                    .set("font-size", "14px");

            Button removeButton = new Button("❌ Remove Role", e -> {
                allRoles.remove(role);
                displayShopRoles(allRoles);
            });

            Button addRoleButton = new Button("➕ Add Permissions", e -> {
                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Add Permissions");

                ComboBox<PermissionsEnum> permissionsFieldD = new ComboBox<>("Permissions");
                permissionsFieldD.setItems(PermissionsEnum.values());

                Button confirmButton = new Button("Confirm", ev -> {
                    // TODO: Add call to backend service to create role
                });

                Button cancelButton = new Button("Cancel", ev -> dialog.close());

                dialog.add(
                        new VerticalLayout(
                                permissionsFieldD,
                                new HorizontalLayout(confirmButton, cancelButton)));

                dialog.open();
            });

            roleCard.add(nameField, permissionsField, new HorizontalLayout(addRoleButton, removeButton));
            rolesContainer.add(roleCard);
        }
    }

    private void displayShopDiscounts(List<DiscountDTO> discounts) {
        discountVerticalLayout.removeAll();

        if (discounts.isEmpty()) {
            Span noPolicies = new Span("No policies available for current filters.");
            noPolicies.getStyle().set("color", "red").set("font-size", "18px").set("font-weight", "bold");
            discountVerticalLayout.add(noPolicies);
            return;
        }

        for (DiscountDTO discount : discounts) {
            VerticalLayout discountCard = new VerticalLayout();
            discountCard.setWidth("100%");
            discountCard.getStyle()
                    .set("border", "1px solid #ccc")
                    .set("border-radius", "8px")
                    .set("padding", "10px")
                    .set("box-shadow", "0 2px 5px rgba(0,0,0,0.05)")
                    .set("margin-bottom", "10px")
                    .set("background-color", "#f9f9f9");

            TextField discountNameField = null;
            if (discount.getItemName() != null) {
                discountNameField = new TextField("Item Name", discount.getItemName());
                discountNameField.setValue(discount.getItemName());
                discountNameField.setReadOnly(true);
            } else if (discount.getItemCategory() != null) {
                discountNameField = new TextField("Category", discount.getItemCategory().toString());
                discountNameField.setValue(discount.getItemCategory().toString());
                discountNameField.setReadOnly(true);
            } else {
                discountNameField = new TextField("Global Discount", "All Items");
                discountNameField.setValue("All Items");
                discountNameField.setReadOnly(true);
            }
            NumberField percentage = new NumberField("Discount Percentage", discount.getPercentage().toString());
            percentage.setValue(discount.getPercentage().doubleValue());
            percentage.setReadOnly(true);

            Button removeButton = new Button("❌ Remove Policy", e -> {
                allDiscounts.remove(discount);
                displayShopDiscounts(discounts);
            });

            discountCard.add(discountNameField, percentage, removeButton);
            discountVerticalLayout.add(discountCard);
        }
    }
}