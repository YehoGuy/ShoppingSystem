package UI;

import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout.Alignment;

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

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import DTOs.ItemDTO;
import Domain.ItemCategory;

@Route(value = "items", layout = AppLayoutBasic.class)
public class ItemSearchView extends VerticalLayout implements BeforeEnterObserver {
    private List<ItemDTO> allItems = new ArrayList<>();
    private List<ItemDTO> filteredItems = new ArrayList<>();
    private VerticalLayout itemsContainer; // <--- FIELD REFERENCE

    private ComboBox<ItemCategory> categoryFilter;
    private NumberField minPriceField;
    private NumberField maxPriceField;
    private NumberField minRatingField;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String ITEM_URL = "http://localhost:8080/api/items";
    private final String SHOP_URL = "http://localhost:8080/api/shops";

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    public ItemSearchView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setSpacing(true);
        setPadding(true);

        getItems();

        H1 title = new H1("Available Items");
        title.getStyle().set("margin-bottom", "10px");
        add(title);

        VerticalLayout filtersLayout = setupFilters();

        TextField searchField = new TextField();
        searchField.setPlaceholder("Search items...");
        searchField.addValueChangeListener(e -> searchItems(e.getValue()));
        searchField.setWidth("300px");
        add(searchField);

        itemsContainer = new VerticalLayout(); // <--- set reference
        itemsContainer.setWidth("80%");
        itemsContainer.setHeight("70vh");
        itemsContainer.getStyle().set("overflow", "auto");

        HorizontalLayout content = new HorizontalLayout(filtersLayout, itemsContainer);
        content.setWidthFull();
        content.setHeightFull();
        content.setFlexGrow(1, itemsContainer);
        add(content);

        displayItems(filteredItems);
    }

    private void getItems() {
        String url = ITEM_URL + "/all";
        ResponseEntity<ItemDTO[]> response = restTemplate.getForEntity(url, ItemDTO[].class);
        if (response.getStatusCode() == HttpStatus.OK) {
            ItemDTO[] items = response.getBody();
            if (items != null) {
                allItems.clear();
                for (ItemDTO item : items) {
                    allItems.add(item);
                }
                filteredItems.addAll(allItems); // Initially, show all items
            }
        } else {
            Notification.show("Failed to fetch items: " + response.getStatusCode());
        }
    }

    private VerticalLayout setupFilters() {
        categoryFilter = new ComboBox<>("Category");
        categoryFilter.setItems(ItemCategory.values());
        categoryFilter.setClearButtonVisible(true);

        minPriceField = new NumberField("Min Price");
        minPriceField.setPlaceholder("e.g. 1.0");
        minPriceField.setWidth("100px");

        maxPriceField = new NumberField("Max Price");
        maxPriceField.setPlaceholder("e.g. 20.0");
        maxPriceField.setWidth("100px");

        minRatingField = new NumberField("Min Rating");
        minRatingField.setPlaceholder("e.g. 3.0");
        minRatingField.setWidth("100px");

        Button applyFiltersButton = new Button("Apply Filters", e -> getFilteredItems("",
                categoryFilter.getValue(), new ArrayList<>(),
                minPriceField.getValue() != null ? minPriceField.getValue().intValue() : null,
                maxPriceField.getValue() != null ? maxPriceField.getValue().intValue() : null,
                minRatingField.getValue(), null));

        VerticalLayout filtersLayout = new VerticalLayout(
                categoryFilter, minPriceField, maxPriceField, minRatingField, applyFiltersButton);
        filtersLayout.setSpacing(true);
        filtersLayout.setPadding(true);
        filtersLayout.setWidth("250px");
        add(filtersLayout);
        return filtersLayout; // Return the layout for further use if needed
    }

    private void getFilteredItems(String name, ItemCategory category, List<String> keywords,
            Integer minPrice, Integer maxPrice, Double minProductRating, Double minShopRating) {
        String url = SHOP_URL
                + "/search?name={name}&category={category}&keywords={keywords}&minPrice={minPrice}&maxPrice={maxPrice}&minProductRating={minProductRating}&minShopRating={minShopRating}";
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("category", category);
        params.put("keywords", keywords);
        params.put("minPrice", minPrice);
        params.put("maxPrice", maxPrice);
        params.put("minProductRating", minProductRating);
        params.put("minShopRating", minShopRating);
        ResponseEntity<ItemDTO[]> response = restTemplate.getForEntity(url, ItemDTO[].class, params);
        if (response.getStatusCode() == HttpStatus.OK) {
            ItemDTO[] items = response.getBody();
            if (items != null) {
                filteredItems.clear();
                for (ItemDTO item : items) {
                    filteredItems.add(item);
                }
                displayItems(filteredItems);
            }
        } else {
            Notification.show("Failed to fetch items: " + response.getStatusCode());
        }
    }

    // search by name
    private void searchItems(String query) {
        filteredItems.clear();
        for (ItemDTO item : allItems) {
            if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredItems.add(item);
            }
        }
        displayItems(filteredItems);
    }

    private void displayItems(List<ItemDTO> items) {
        itemsContainer.removeAll();

        if (items.isEmpty()) {
            Span noItems = new Span("No items found.");
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

            Span name = new Span("🛒 " + item.getName());
            name.getStyle().set("font-size", "18px").set("font-weight", "600");

            Span description = new Span(item.getDescription());
            Span price = new Span("💰 Price: $" + item.getPrice());
            Span category = new Span("📦 Category: " + item.getCategory());

            Button buyButton = new Button("Buy");
            buyButton.getStyle().set("margin-top", "10px");
            buyButton.addClickListener(e -> {
                // Placeholder: implement actual purchase logic later
                Notification.show("You bought: " + item.getName());
            });

            itemCard.add(name, description, price, category, buyButton);
            itemsContainer.add(itemCard);
        }
    }
}
