package UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.orderedlayout.FlexComponent;

import DTOs.ItemDTO;
import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;
import DomainLayer.Item.ItemCategory;

@Route(value = "shops", layout = AppLayoutBasic.class)
public class ShopSearchView extends VerticalLayout {
    private List<ShopDTO> allShops = new ArrayList<>(); // Store the full list of shops
    private List<ShopDTO> filteredShops = new ArrayList<>(); // Store the filtered list

    public ShopSearchView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START); // Ensure content is aligned from the top
        setSpacing(true);
        setPadding(true);

        // Sample data for demonstration
        List<ShopReviewDTO> reviews1 = new ArrayList<>();
        reviews1.add(new ShopReviewDTO(1, 5, "Great service!"));
        reviews1.add(new ShopReviewDTO(2, 4, "Good selection of products."));
        List<ShopReviewDTO> reviews2 = new ArrayList<>();
        reviews2.add(new ShopReviewDTO(3, 3, "Average experience."));
        reviews2.add(new ShopReviewDTO(4, 2, "Not very helpful staff."));
        Map<ItemDTO, Integer> items1 = new HashMap<>();
        items1.put(new ItemDTO(0, "banana", "a good banana", ItemCategory.GROCERY), 5);
        items1.put(new ItemDTO(1, "apple", "a good apple", ItemCategory.GROCERY), 10);
        Map<ItemDTO, Integer> items2 = new HashMap<>();
        items2.put(new ItemDTO(2, "carrot", "a good carrot", ItemCategory.GROCERY), 7);
        items2.put(new ItemDTO(3, "potato", "a good potato", ItemCategory.GROCERY), 12);

        allShops.add(new ShopDTO("shop A", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop B", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop C", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop D", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop E", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop F", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop G", items1, items1, reviews1));
        allShops.add(new ShopDTO("shop H", items2, items2, reviews2));
        allShops.add(new ShopDTO("shop I", items1, items1, reviews1));

        filteredShops.addAll(allShops); // Initially, show all shops

        // Create and add the title
        H1 title = new H1("Available Shops");
        title.getStyle().set("margin-bottom", "10px");
        add(title);

        // Create and add the search bar
        TextField searchField = new TextField();
        searchField.setPlaceholder("Search shops...");
        searchField.addValueChangeListener(e -> filterShops(e.getValue()));
        searchField.setWidth("300px");
        add(searchField); // Ensure this is added above the shops list

        // Create a scrollable container for the shops list
        VerticalLayout shopsContainer = new VerticalLayout();
        shopsContainer.setWidth("80%");
        shopsContainer.setHeight("70vh"); // 70% of the vertical height of the page
        shopsContainer.getStyle().set("overflow", "auto");

        // Display shops initially
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
                UI.getCurrent().navigate("shop/" + shop.getName().replaceAll(" ", "-"));
            });
            shopName.getElement().executeJs(
                    "this.addEventListener('mouseover', () => this.style.color = '#0056b3');" +
                            "this.addEventListener('mouseout', () => this.style.color = '#007bff');");

            // Toggleable review section
            Details reviewDetails = new Details("Show Reviews", createReviewList(shop.getReviews()));
            reviewDetails.setOpened(false); // collapsed by default

            shopLayout.add(shopName, reviewDetails);
            shopsContainer.add(shopLayout);
        });
    }

    private Component createReviewList(List<ShopReviewDTO> reviews) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.getStyle().set("width", "100%");

        for (ShopReviewDTO review : reviews) {
            VerticalLayout reviewCard = new VerticalLayout();
            reviewCard.setSpacing(false);
            reviewCard.setPadding(true);
            reviewCard.getStyle()
                    .set("border", "1px solid #ddd")
                    .set("border-radius", "8px")
                    .set("box-shadow", "0 2px 5px rgba(0,0,0,0.05)")
                    .set("background-color", "#fafafa")
                    .set("padding", "10px")
                    .set("margin-bottom", "10px");

            Span reviewText = new Span("📝 " + review.getReviewText());
            reviewText.getStyle()
                    .set("font-size", "16px")
                    .set("font-weight", "500");

            Span rating = new Span("⭐ Rating: " + review.getRating());
            rating.getStyle()
                    .set("color", "#f39c12")
                    .set("font-weight", "600");

            reviewCard.add(reviewText, rating);
            layout.add(reviewCard);
        }

        return layout;
    }
}
