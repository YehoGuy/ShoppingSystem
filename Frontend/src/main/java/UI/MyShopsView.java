package UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import DTOs.ItemDTO;
import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;
import Domain.ItemCategory;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

@Route(value = "myshops", layout = AppLayoutBasic.class)
public class MyShopsView extends VerticalLayout {

    private List<ShopDTO> allShops = new ArrayList<>(); // Store the full list of shops
    private List<ShopDTO> filteredShops = new ArrayList<>(); // Store the filtered list

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    public MyShopsView() {

        List<ShopReviewDTO> reviews1 = new ArrayList<>();
        reviews1.add(new ShopReviewDTO(1, 5, "Great service!", "shop A"));
        reviews1.add(new ShopReviewDTO(2, 4, "Good selection of products.", "shop A"));
        List<ShopReviewDTO> reviews2 = new ArrayList<>();
        reviews2.add(new ShopReviewDTO(3, 3, "Average experience.", "shop B"));
        reviews2.add(new ShopReviewDTO(4, 2, "Not very helpful staff.", "shop B"));
        Map<ItemDTO, Integer> items1 = new HashMap<>();
        items1.put(new ItemDTO(0, "banana", "a good banana", 10.0, ItemCategory.GROCERY), 5);
        items1.put(new ItemDTO(1, "apple", "a good apple", 10.0, ItemCategory.GROCERY), 10);
        Map<ItemDTO, Integer> items2 = new HashMap<>();
        items2.put(new ItemDTO(2, "carrot", "a good carrot", 10.0, ItemCategory.GROCERY), 7);
        items2.put(new ItemDTO(3, "potato", "a good potato", 10.0, ItemCategory.GROCERY), 12);

        allShops.add(new ShopDTO("very very very very long shop name that wont fit", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop A", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop B", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop C", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop D", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop E", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop F", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop G", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop H", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop I", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop J", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop K", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop L", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop M", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop N", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop O", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop P", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop Q", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop R", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop S", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop T", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop U", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop V", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop W", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop X", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop Y", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop Z", items2, items2, reviews2));

        filteredShops.addAll(allShops); // Initially, show all shops

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        add(new H1("My Shops")); // Add a title

        Button addShopButton = new Button("Add New Shop");
        addShopButton.addClickListener(e -> {
            Dialog dialog = new Dialog();
            dialog.setModal(true);
            dialog.setDraggable(true);

            TextField nameField = new TextField("Shop Name");
            nameField.setWidth("100%");

            Button confirmButton = new Button("Create Shop", event -> {
                String shopName = nameField.getValue();
                if (!shopName.isEmpty()) {
                    // Add shop creation logic here
                    dialog.close();
                }
            });

            Button cancelButton = new Button("Cancel", event -> dialog.close());

            dialog.add(new VerticalLayout(
                    nameField,
                    new HorizontalLayout(confirmButton, cancelButton)));

            dialog.open();
        });
        addShopButton.getStyle()
                .set("margin-left", "10px")
                .set("background-color", "#007bff")
                .set("color", "white");

        TextField searchField = new TextField();
        searchField.setPlaceholder("Search shops...");
        searchField.addValueChangeListener(e -> filterShops(e.getValue()));
        searchField.setWidth("300px");

        // Create a horizontal layout for title and button
        HorizontalLayout headerLayout = new HorizontalLayout(searchField, new Span(), addShopButton);
        headerLayout.setAlignItems(Alignment.CENTER);
        add(headerLayout);

        VerticalLayout shopsContainer = new VerticalLayout();
        shopsContainer.setWidth("80%");
        shopsContainer.setHeight("70vh"); // 70% of the vertical height of the page
        shopsContainer.getStyle()
                .set("overflow", "auto")
                .set("display", "grid")
                .set("grid-template-columns", "repeat(4, 1fr)")
                .set("gap", "20px")
                .set("padding", "20px");

        // Display shops initially
        // Create a button for adding a new shop

        displayShops(filteredShops, shopsContainer);

        // Add the scrollable shops container
        add(shopsContainer);
    }

    private void filterShops(String query) {
        filteredShops.clear(); // Clear current filtered list
        for (ShopDTO shop : allShops) {
            if (shop.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredShops.add(shop);
            }
        }
        // Update the displayed list based on the filtered shops
        displayShops(filteredShops, (VerticalLayout) getChildren().toArray()[2]);
    }

    private void displayShops(List<ShopDTO> shops, VerticalLayout shopsContainer) {
        shopsContainer.removeAll(); // Clear existing components before re-rendering

        // Display filtered shops
        shops.forEach(shop -> {
            VerticalLayout shopLayout = new VerticalLayout();
            shopLayout.setWidth("100%");
            shopLayout.getStyle().set("border", "1px solid #ccc").set("padding", "15px");

            Span shopName = new Span(shop.getName());
            shopName.getStyle()
                    .set("color", "#007bff") // Bootstrap blue
                    .set("cursor", "pointer")
                    .set("text-decoration", "none")
                    .set("font-size", "20px")
                    .set("font-weight", "600")
                    .set("transition", "color 0.2s");

            shopName.addClickListener(e -> {
                UI.getCurrent().navigate("editShop/" + shop.getName());
            });
            shopName.getElement().executeJs(
                    "this.addEventListener('mouseover', () => this.style.color = '#0056b3');" +
                            "this.addEventListener('mouseout', () => this.style.color = '#007bff');");

            shopLayout.add(shopName);
            shopsContainer.add(shopLayout);
        });
    }

}
