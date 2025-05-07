package UI;

import java.util.*;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import DTOs.ItemDTO;
import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;
import DomainLayer.Item.ItemCategory;

@Route(value = "shop", layout = AppLayoutBasic.class)
public class ShopView extends VerticalLayout implements HasUrlParameter<String> {

    private VerticalLayout itemsContainer;
    private ShopDTO selectedShop;

    private ComboBox<ItemCategory> categoryFilter;
    private NumberField minPriceField;
    private NumberField maxPriceField;
    private TextField nameSearchField;

    private List<ItemDTO> allItems = new ArrayList<>(); // Store the full list of items


    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String shopName) {
        if (shopName == null || shopName.isEmpty()) {
            add(new Span("No shop selected."));
            return;
        }

        loadShopData(shopName);

        if (selectedShop == null) {
            add(new Span("Shop '" + shopName + "' not found."));
            return;
        }

        showShopView();
    }

    private void loadShopData(String shopName) {
        selectedShop = new ShopDTO(
                "shop-A",
                Map.of(
                        new ItemDTO(1, "Banana", "Fresh yellow banana", 2.5, ItemCategory.GROCERY), 5,
                        new ItemDTO(2, "Apple", "Juicy red apple", 3.0, ItemCategory.GROCERY), 10
                ),
                Map.of(
                        new ItemDTO(1, "Banana", "Fresh yellow banana", 2.5, ItemCategory.GROCERY), 2,
                        new ItemDTO(2, "Apple", "Juicy red apple", 3.0, ItemCategory.GROCERY), 3
                ),
                List.of(
                        new ShopReviewDTO(1, 5, "Great service!"),
                        new ShopReviewDTO(2, 4, "Good selection of products.")
                )
        );

        if (!selectedShop.getName().equalsIgnoreCase(shopName)) {
            selectedShop = null;
        }
    }

    private void showShopView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setSpacing(true);
        setPadding(true);

        allItems.clear();
        allItems.addAll(selectedShop.getItems().keySet());

        H1 title = new H1("🛍️ Welcome to " + selectedShop.getName());
        title.getStyle().set("margin-bottom", "10px");
        add(title);

        Span avgRating = new Span("⭐ Average Rating: " + calculateAverageRating(selectedShop.getReviews()));
        add(avgRating);

        itemsContainer = new VerticalLayout();
        itemsContainer.setWidth("80%");
        itemsContainer.setHeight("70vh");
        itemsContainer.getStyle().set("overflow", "auto");

        HorizontalLayout content = new HorizontalLayout(setupFilters(), itemsContainer);
        content.setWidthFull();
        add(content);

        displayShopItems(allItems);// show all items at first
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
        //implement with waf        
    }

    private void displayShopItems(List<ItemDTO> items) {
        itemsContainer.removeAll();

        if (items.isEmpty()) {
            Span noItems = new Span("No items available for current filters.");
            noItems.getStyle().set("color", "red").set("font-size", "18px").set("font-weight", "bold");
            itemsContainer.add(noItems);
            return;
        }

        Map<ItemDTO, Integer> prices= selectedShop.getItems().entrySet().stream()
                .filter(entry -> items.contains(entry.getKey().getId()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (ItemDTO item : allItems) {

            int price = prices.getOrDefault(item, 0);

            VerticalLayout itemCard = new VerticalLayout();
            itemCard.setWidth("100%");
            itemCard.getStyle()
                    .set("border", "1px solid #ccc")
                    .set("border-radius", "8px")
                    .set("padding", "10px")
                    .set("box-shadow", "0 2px 5px rgba(0,0,0,0.05)")
                    .set("margin-bottom", "10px")
                    .set("background-color", "#f9f9f9");

            Span name = new Span("📦 " + item.getName());
            name.getStyle().set("font-size", "18px").set("font-weight", "600");

            Span desc = new Span(item.getDescription());
            Span priceSpan = new Span("💰 Price: $" + price);

            Button addCartButton = new Button("Add to Cart");
            addCartButton.getStyle().set("margin-top", "10px");
            addCartButton.addClickListener(e -> {
                Notification.show("Added to cart: " + item.getName());
            });

            itemCard.add(name, desc, priceSpan, addCartButton);
            itemsContainer.add(itemCard);
        }
    }

    private double calculateAverageRating(List<ShopReviewDTO> reviews) {
        if (reviews == null || reviews.isEmpty()) return 0.0;
        return reviews.stream().mapToDouble(ShopReviewDTO::getRating).average().orElse(0.0);
    }
}
