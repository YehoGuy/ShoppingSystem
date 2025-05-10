package UI;

import DTOs.ItemDTO;
import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;
import DomainLayer.Item.ItemCategory;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.*;

@Route(value = "admin-shop", layout = AppLayoutBasic.class)
public class AdminShopView extends VerticalLayout {

    private ShopDTO shop;
    private VerticalLayout itemsContainer;

    private ComboBox<ItemCategory> categoryFilter;
    private NumberField minPriceField;
    private NumberField maxPriceField;
    private TextField nameSearchField;

    private List<ItemDTO> allItems;

    public AdminShopView() {
        loadShopData("shop-A");

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setSpacing(true);
        setPadding(true);

        H1 title = new H1("🛍️ Admin View – " + shop.getName());
        add(title);

        Span avgRating = new Span("⭐ Average Rating: " + calculateAverageRating(shop.getReviews()));
        add(avgRating);

        allItems = new ArrayList<>(shop.getItems().keySet());

        itemsContainer = new VerticalLayout();
        itemsContainer.setWidth("80%");
        itemsContainer.setHeight("70vh");
        itemsContainer.getStyle().set("overflow", "auto");

        HorizontalLayout content = new HorizontalLayout(setupFilters(), itemsContainer);
        content.setWidthFull();
        add(content);

        displayShopItems(allItems);
    }

    private void loadShopData(String shopName) {
        shop = new ShopDTO(
                shopName,
                new LinkedHashMap<>(Map.of(
                        new ItemDTO(1, "Banana", "Fresh yellow banana", 2.5, ItemCategory.GROCERY), 10,
                        new ItemDTO(2, "Apple", "Juicy red apple", 3.0, ItemCategory.GROCERY), 15
                )),
                new LinkedHashMap<>(Map.of(
                        new ItemDTO(1, "Banana", "Fresh yellow banana", 2.5, ItemCategory.GROCERY), 2,
                        new ItemDTO(2, "Apple", "Juicy red apple", 3.0, ItemCategory.GROCERY), 3
                )),
                List.of(
                        new ShopReviewDTO(1, 5, "Excellent!"),
                        new ShopReviewDTO(2, 4, "Great quality.")
                )
        );
    }

    private VerticalLayout setupFilters() {
        nameSearchField = new TextField("Search");
        nameSearchField.setPlaceholder("e.g. apple");

        categoryFilter = new ComboBox<>("Category");
        categoryFilter.setItems(ItemCategory.values());
        categoryFilter.setClearButtonVisible(true);

        minPriceField = new NumberField("Min Price");
        maxPriceField = new NumberField("Max Price");

        Button applyFiltersButton = new Button("Apply Filters", e -> applyFilters());

        VerticalLayout filtersLayout = new VerticalLayout(
                nameSearchField,
                categoryFilter,
                minPriceField,
                maxPriceField,
                applyFiltersButton
        );
        filtersLayout.setSpacing(true);
        filtersLayout.setPadding(true);
        filtersLayout.setWidth("250px");
        return filtersLayout;
    }

    private void applyFilters() {
        List<ItemDTO> filtered = new ArrayList<>(allItems);
        String query = nameSearchField.getValue() != null ? nameSearchField.getValue().toLowerCase() : "";
        ItemCategory category = categoryFilter.getValue();
        Double minPrice = minPriceField.getValue();
        Double maxPrice = maxPriceField.getValue();

        filtered.removeIf(item -> !item.getName().toLowerCase().contains(query));
        if (category != null)
            filtered.removeIf(item -> !category.equals(item.getCategory()));
        if (minPrice != null)
            filtered.removeIf(item -> shop.getPrices().getOrDefault(item, 0) < minPrice);
        if (maxPrice != null)
            filtered.removeIf(item -> shop.getPrices().getOrDefault(item, 0) > maxPrice);

        displayShopItems(filtered);
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
            TextField descField = new TextField("Description", item.getDescription());
            NumberField priceField = new NumberField("Price");
            priceField.setValue(shop.getPrices().getOrDefault(item, 0).doubleValue());


            Button saveButton = new Button("💾 Save Changes", e -> {
                ItemDTO updatedItem = new ItemDTO(
                        item.getId(),
                        nameField.getValue(),
                        descField.getValue(),
                        item.getPrice(), // keeping the base price as-is
                        item.getCategoryEnum() // unchanged category
                );

                updateShopItem(updatedItem, priceField.getValue().intValue());
                Notification.show("Updated item: " + updatedItem.getName());
                getUI().ifPresent(ui -> ui.getPage().reload()); // force refresh
            });

            
            Button removeButton = new Button("❌ Remove", e -> {
                shop.getItems().remove(item);
                shop.getPrices().remove(item);
                allItems.remove(item);
                displayShopItems(allItems);
            });

            itemCard.add(nameField, descField, priceField, new HorizontalLayout(saveButton, removeButton));
            itemsContainer.add(itemCard);
        }
    }

    private double calculateAverageRating(List<ShopReviewDTO> reviews) {
        if (reviews == null || reviews.isEmpty()) return 0.0;
        return reviews.stream().mapToDouble(ShopReviewDTO::getRating).average().orElse(0.0);
    }


    private void updateShopItem(ItemDTO updatedItem, int newPrice) {
    // Remove any existing item with the same ID
    shop.getItems().keySet().removeIf(i -> i.getId() == updatedItem.getId());
    shop.getPrices().keySet().removeIf(i -> i.getId() == updatedItem.getId());

    // Reinsert updated item (mock quantity 10)
    shop.getItems().put(updatedItem, 10);
    shop.getPrices().put(updatedItem, newPrice);

    // Replace in allItems list
    allItems.removeIf(i -> i.getId() == updatedItem.getId());
    allItems.add(updatedItem);
}

}
