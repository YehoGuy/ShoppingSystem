package UI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLink;

import DTOs.ItemDTO;
import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;
import DomainLayer.Item.ItemCategory;

@Route(value = "shops", layout = AppLayoutBasic.class)
public class ShopSearchView extends VerticalLayout {
    public ShopSearchView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(true);
        setPadding(true);
        List<ShopDTO> shops = new ArrayList<>(); // Replace with actual shop data retrieval
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
        shops.add(new ShopDTO("shop A", items1, items1, reviews1));
        shops.add(new ShopDTO("shop B", items2, items2, reviews2));
        H1 title = new H1("Available Shops");
        add(title);
        shops.forEach(shop -> {
            VerticalLayout shopLayout = new VerticalLayout();
            shopLayout.setWidth("80%");
            shopLayout.getStyle().set("border", "1px solid #ccc").set("padding", "15px");

            RouterLink shopName = new RouterLink("", ShopView.class, shop.getName().replaceAll(" ", "-"));
            shopName.add(new H2(shop.getName()));
            // H2 shopName = new H2(shop.getName());
            // Toggleable review section
            Details reviewDetails = new Details("Show Reviews", createReviewList(shop.getReviews()));
            reviewDetails.setOpened(false); // collapsed by default

            shopLayout.add(shopName, reviewDetails);
            add(shopLayout);
        });
    }

    private Component createReviewList(List<ShopReviewDTO> reviews) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);

        for (ShopReviewDTO review : reviews) {
            Div reviewDiv = new Div();
            reviewDiv.getStyle().set("border-bottom", "1px solid #eee").set("padding", "8px 0");
            reviewDiv.add(new Span("Review: " + review.getReviewText()));
            reviewDiv.add(new Html("<br/>"));
            reviewDiv.add(new Span("Rating: " + review.getRating()));
            layout.add(reviewDiv);
        }

        return layout;
    }

}
