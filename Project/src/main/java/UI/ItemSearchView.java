package UI;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import DTOs.ItemDTO;
import DomainLayer.Item.ItemCategory;

@Route(value = "items", layout = AppLayoutBasic.class)
public class ItemSearchView extends VerticalLayout {
    private List<ItemDTO> allItems = new ArrayList<>();
    private List<ItemDTO> filteredItems = new ArrayList<>();

    public ItemSearchView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setSpacing(true);
        setPadding(true);

        // Sample data
        allItems.add(new ItemDTO(1, "Banana", "Fresh yellow banana", 2.5, ItemCategory.GROCERY));
        allItems.add(new ItemDTO(2, "Apple", "Juicy red apple", 3.0, ItemCategory.GROCERY));
        allItems.add(new ItemDTO(3, "Potato", "Organic potato", 1.2, ItemCategory.GROCERY));
        allItems.add(new ItemDTO(4, "Shampoo", "For dry hair", 15.0, ItemCategory.BEAUTY));
        filteredItems.addAll(allItems);

        H1 title = new H1("Available Items");
        title.getStyle().set("margin-bottom", "10px");
        add(title);

        TextField searchField = new TextField();
        searchField.setPlaceholder("Search items...");
        searchField.addValueChangeListener(e -> filterItems(e.getValue()));
        searchField.setWidth("300px");
        add(searchField);

        VerticalLayout itemsContainer = new VerticalLayout();
        itemsContainer.setWidth("80%");
        itemsContainer.setHeight("70vh");
        itemsContainer.getStyle().set("overflow", "auto");
        add(itemsContainer);

        displayItems(filteredItems, itemsContainer);
    }

    private void filterItems(String query) {
        filteredItems.clear();
        for (ItemDTO item : allItems) {
            if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredItems.add(item);
            }
        }

        VerticalLayout itemsContainer = (VerticalLayout) getChildren().toArray()[3]; // scroll container
        displayItems(filteredItems, itemsContainer);
    }

    private void displayItems(List<ItemDTO> items, VerticalLayout container) {
        container.removeAll();
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

            itemCard.add(name, description, price, category);
            container.add(itemCard);
        }
    }
}
