package UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Vertical;

import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;

@Route(value = "shops", layout = AppLayoutBasic.class)

public class ShopSearchView extends VerticalLayout implements BeforeEnterObserver {

    @Value("${url.api}/shops/all")
    private String shopsApiUrl;

    @Value("${url.api}/shops")
    private String shopApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private final List<ShopDTO> allShops = new ArrayList<>();
    private final List<ShopDTO> filteredShops = new ArrayList<>();
    private final VerticalLayout shopsContainer = new VerticalLayout();

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // Ensure userId is set in session
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            event.forwardTo("");
            return;
        }

        loadShops(token);
        displayShops(filteredShops);
        handleSuspence();
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    public ShopSearchView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setSpacing(true);
        setPadding(true);

        // Title
        add(new H1("Available Shops"));

        // Search bar
        TextField searchField = new TextField();
        searchField.setPlaceholder("Search shops...");
        searchField.setWidth("300px");
        searchField.addValueChangeListener(e -> filterShops(e.getValue()));
        add(searchField);

        // Shops container
        shopsContainer.setWidth("80%");
        shopsContainer.setHeight("70vh");
        shopsContainer.getStyle().set("overflow", "auto");
        add(shopsContainer);
    }

    public void loadShops(String token) {
        try {
            String url = shopsApiUrl + "?token=" + token;
            ResponseEntity<ShopDTO[]> response = restTemplate.getForEntity(url, ShopDTO[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                allShops.clear();
                allShops.addAll(Arrays.asList(response.getBody()));
                filteredShops.clear();
                filteredShops.addAll(allShops);
            } else {
                Notification.show("Failed to load shops", 3000,
                        Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("Error loading shops", 5000, Notification.Position.MIDDLE);
        }
    }

    public void filterShops(String query) {
        filteredShops.clear();
        filteredShops.addAll(
                allShops.stream()
                        .filter(s -> s.getName().toLowerCase().contains(query.toLowerCase()))
                        .collect(Collectors.toList()));
        displayShops(filteredShops);
    }

    public void displayShops(List<ShopDTO> shops) {
        shopsContainer.removeAll();
        if (shops.isEmpty()) {
            Notification.show("No shops found.", 3000, Notification.Position.MIDDLE);
            return;
        }
        shops.forEach(shop -> {
            VerticalLayout shopLayout = createShopLayout(shop);
            shopsContainer.add(shopLayout);
        });
        shopsContainer.setAlignItems(Alignment.CENTER);
    }

    private VerticalLayout createShopLayout(ShopDTO shop) {
        VerticalLayout shopLayout = new VerticalLayout();
        shopLayout.setWidth("100%");
        shopLayout.getStyle()
                .set("border", "1px solid #ccc")
                .set("padding", "15px");

        // Clickable shop name
        H1 shopName = new H1(shop.getName());
        shopName.getStyle()
                .set("cursor", "pointer")
                .set("margin", "0");
        shopName.addClickListener(e -> UI.getCurrent().navigate("shop/" + shop.getShopId()));
        shopLayout.add(shopName);

        VerticalLayout reviewsLayout = new VerticalLayout();
        reviewsLayout.setHeight("600px");
        VerticalLayout reviews = displayReviews(shop.getReviews());
        reviewsLayout.add(reviews);

        // Add review section
        TextArea reviewTextArea = new TextArea("Write a review");
        reviewTextArea.setPlaceholder("Enter your review here...");
        reviewTextArea.setSizeFull();
        NumberField ratingField = new NumberField("Rating (1-5)");
        ratingField.setPlaceholder("Enter rating (1-5)");
        ratingField.setMin(1);
        ratingField.setMax(5);
        ratingField.setStep(1);
        ratingField.setSizeFull();
        Button addReviewButton = new Button("Add Review", e -> {
            if (reviewTextArea.getValue().isEmpty() || ratingField.getValue() == null) {
                Notification.show("Please enter a review and a rating", 3000, Notification.Position.MIDDLE);
                return;
            }
            sendReview(shop, reviewTextArea.getValue(), ratingField.getValue().intValue());
        });
        VerticalLayout addReview = new VerticalLayout();
        addReview.setSizeFull();
        reviewTextArea.getStyle().set("border", "1px solid red");
        ratingField.getStyle().set("border", "1px solid blue");
        addReview.getStyle().set("border", "1px solid green");

        addReview.add(reviewTextArea);
        addReview.add(ratingField);
        addReview.add(addReviewButton);

        reviewsLayout.add(addReview);
        shopLayout.add(reviewsLayout);
        return shopLayout;
    }

    private void sendReview(ShopDTO shop, String reviewText, int rating) {
        String url = shopApiUrl + "/" + shop.getShopId() + "/reviews?rating=" + rating
                + "&reviewText=" + reviewText + "&token=" + VaadinSession.getCurrent().getAttribute("authToken");
        ResponseEntity<?> response = restTemplate.postForEntity(url, null, Void.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            Notification.show("Review added successfully", 3000, Notification.Position.MIDDLE);
        } else {
            Notification.show("Failed to add review", 3000, Notification.Position.MIDDLE);
        }
    }

    private VerticalLayout displayReviews(List<ShopReviewDTO> reviews) {
        VerticalLayout reviewsLayout = new VerticalLayout();
        reviewsLayout.setWidth("100%");

        if (reviews == null || reviews.isEmpty())
            return reviewsLayout;

        VerticalLayout visibleReviewsLayout = new VerticalLayout();
        visibleReviewsLayout.setWidthFull();
        reviewsLayout.add(visibleReviewsLayout);

        Button showMoreButton = new Button("Show More");

        // Index tracker
        AtomicInteger currentIndex = new AtomicInteger(0);

        // Function to load more reviews
        Runnable loadMoreReviews = () -> {
            int start = currentIndex.get();
            int end = Math.min(start + 3, reviews.size());

            for (int i = start; i < end; i++) {
                ShopReviewDTO review = reviews.get(i);
                H2 reviewText = new H2(review.getReviewText());
                reviewText.getStyle().set("margin", "5px 0");
                H3 rating = new H3("Rating: " + review.getRating());
                rating.getStyle().set("margin", "0 0 5px 0");
                reviewText.add(rating);
                visibleReviewsLayout.add(reviewText);
            }

            currentIndex.set(end);

            if (end >= reviews.size()) {
                showMoreButton.setVisible(false);
            }
        };

        // Load initial reviews
        loadMoreReviews.run();

        showMoreButton.addClickListener(e -> loadMoreReviews.run());
        reviewsLayout.add(showMoreButton);

        return reviewsLayout;
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
        String url = "http://localhost:8080/api/users" + "/" + userId + "/suspension?token=" + token;
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }
}
