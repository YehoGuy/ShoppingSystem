package UI;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;

@Route("shops")
public class ShopSearchView extends VerticalLayout {
    public ShopSearchView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(true);
        setPadding(true);
        List<ShopDTO> shops = new ArrayList<>(); // Replace with actual shop data retrieval
        H1 title = new H1("Available Shops");
        add(title);
        shops.forEach(shop -> {
            VerticalLayout shopLayout = new VerticalLayout();
            shopLayout.setWidth("80%");
            shopLayout.getStyle().set("border", "1px solid #ccc").set("padding", "15px");

            H2 shopName = new H2(shop.getName());
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
