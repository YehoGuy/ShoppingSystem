package UI;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
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
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

import DTOs.ItemDTO;
import DTOs.ItemReviewDTO;
import Domain.ItemCategory;

@Route(value = "items", layout = AppLayoutBasic.class)

public class ItemSearchView extends VerticalLayout implements BeforeEnterObserver {
    private List<ItemDTO> allItems = new ArrayList<>();
    private List<ItemDTO> filteredItems = new ArrayList<>();
    private VerticalLayout itemsContainer; // <--- FIELD REFERENCE

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${url.api}/items")
    private String URL;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }

        handleSuspence();
    }

    private String getUserId() {
        return VaadinSession.getCurrent().getAttribute("userId").toString();
    }

    public ItemSearchView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setSpacing(true);
        setPadding(true);
    }

    @PostConstruct
    private void init() {
        getItems();

        H1 title = new H1("Available Items");
        title.getStyle().set("margin-bottom", "10px");
        add(title);

        TextField searchField = new TextField();
        searchField.setPlaceholder("Search items...");
        searchField.addValueChangeListener(e -> searchItems(e.getValue()));
        searchField.setWidth("300px");
        add(searchField);

        itemsContainer = new VerticalLayout(); // <--- set reference
        itemsContainer.setWidth("80%");
        itemsContainer.setHeight("70vh");
        itemsContainer.getStyle().set("overflow", "auto");

        HorizontalLayout content = new HorizontalLayout(itemsContainer);
        content.setWidthFull();
        content.setHeightFull();
        content.setFlexGrow(1, itemsContainer);
        add(content);

        displayItems(filteredItems);
    }

    private void getItems() {
        String url = URL + "/all?token=" + VaadinSession.getCurrent().getAttribute("authToken");
        ResponseEntity<List<ItemDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ItemDTO>>() {
                });

        if (response.getStatusCode().is2xxSuccessful()) {
            List<ItemDTO> items = response.getBody();
            allItems.clear();
            if (items != null && !items.isEmpty()) {
                allItems.addAll(items);
                filteredItems.addAll(allItems);
            } else {
                // add mock items for demo purposes
                ItemDTO item1 = new ItemDTO();
                item1.setId(1);
                item1.setName("Demo Item 1");
                item1.setDescription("This is a demo item.");
                item1.setCategory("GROCERY");
                item1.setAverageRating(4.5);
                item1.setReviews(new ArrayList<>());
                allItems.add(item1);
                filteredItems.add(item1);
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
            Span category = new Span("📦 Category: " + item.getCategory());
            Span averageRating = new Span("⭐ Average Rating: " + item.getAverageRating());

            VerticalLayout reviewsLayout = new VerticalLayout();
            reviewsLayout.setVisible(false); // hidden by default

            // Add reviews to the layout
            for (ItemReviewDTO review : item.getReviews()) {
                VerticalLayout singleReview = new VerticalLayout();
                singleReview.add(
                        new Span("Rating: " + review.getRating()),
                        new Span("Comment: " + review.getReviewText()));
                singleReview.getStyle().set("border", "1px solid #ccc");
                singleReview.getStyle().set("padding", "10px");
                singleReview.getStyle().set("margin-bottom", "10px");
                reviewsLayout.add(singleReview);
            }

            // Show more button
            Button showMoreButton = new Button("Show Reviews");
            showMoreButton.addClickListener(event -> {
                boolean currentlyVisible = reviewsLayout.isVisible();
                reviewsLayout.setVisible(!currentlyVisible);
                showMoreButton.setText(currentlyVisible ? "Show Reviews" : "Hide Reviews");
            });

            itemCard.add(
                    name,
                    description,
                    category,
                    averageRating,
                    reviewsLayout,
                    showMoreButton);
            itemsContainer.add(itemCard);
        }
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
            throw new RuntimeException(
                    "Failed to check admin status: HTTP " + response.getStatusCode().value());
        }
    }
}
