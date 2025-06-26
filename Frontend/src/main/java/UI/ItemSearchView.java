package UI;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import DTOs.ItemDTO;
import DTOs.ItemReviewDTO;


@Route(value = "items", layout = AppLayoutBasic.class)
@JsModule("@vaadin/dialog/vaadin-dialog.js")
@JsModule("@vaadin/number-field/vaadin-number-field.js")
public class ItemSearchView extends BaseView implements BeforeEnterObserver {

    private final RestTemplate rest = new RestTemplate();
    private final List<ItemDTO> allItems      = new ArrayList<>();
    private final List<ItemDTO> filteredItems = new ArrayList<>();
    private final VerticalLayout itemsContainer = new VerticalLayout();
    private final String apiBase;

    public ItemSearchView(@Value("${url.api}") String api) {
        // Animated header: icon + title + arrow
        super("Items", "Discover products", "🛍️", "🔎");
        this.apiBase = api;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // 1) Fetch everything up‐front
        fetchAllItems();

        // 2) Build search bar
        TextField search = new TextField();
        search.setPlaceholder("Search items…");
        search.setWidth("300px");
        search.addValueChangeListener(e -> filterAndDisplay(e.getValue()));

        // 3) Prepare the container for item cards
        itemsContainer.setSizeFull();
        itemsContainer.getStyle()
            .set("overflow", "auto")
            .set("padding", "1rem");

        // 4) Wrap both in a styled “card”
        VerticalLayout card = new VerticalLayout(search, itemsContainer);
        card.addClassName("view-card");
        card.setSizeFull();
        card.setMaxWidth("1000px");
        card.getStyle()
            .set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)")
            .set("padding", "1.5rem");

        // Let the itemsContainer take all leftover space below the search bar
        card.expand(itemsContainer);

        // 5) Add & expand into the view
        add(card);
        expand(card);

        // 6) Initial display
        filterAndDisplay("");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // redirect if not logged in
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
        // hide “add review” if suspended
        handleSuspence();
    }

    private void fetchAllItems() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String url = apiBase + "/items/all?token=" + token;
        ResponseEntity<List<ItemDTO>> resp = rest.exchange(
            url, HttpMethod.GET, null,
            new ParameterizedTypeReference<>() {}
        );
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            allItems.clear();
            allItems.addAll(allItems);
            filteredItems.addAll(allItems);
        } else {
            Notification.show("Failed to fetch items");
        }
    }

    private void filterAndDisplay(String q) {
        filteredItems.clear();
        String lower = q == null ? "" : q.toLowerCase();
        allItems.stream()
                .filter(i -> i.getName().toLowerCase().contains(lower))
                .forEach(filteredItems::add);
        renderCards();
    }

    private void renderCards() {
        itemsContainer.removeAll();
        if (filteredItems.isEmpty()) {
            Span none = new Span("No items found.");
            none.getStyle().set("color", "#c00").set("font-size", "1.2rem");
            itemsContainer.add(none);
            return;
        }
        for (ItemDTO item : filteredItems) {
            VerticalLayout card = new VerticalLayout();
            card.setWidth("100%");
            card.getStyle()
                .set("border-radius", "0.75rem")
                .set("box-shadow", "0 4px 16px rgba(0,0,0,0.08)")
                .set("padding", "1rem")
                .set("margin-bottom", "1rem")
                .set("background", "#fff");

            card.add(
                new Span("🛒 " + item.getName()),
                new Span(item.getDescription()),
                new Span("📦 Category: " + item.getCategory()),
                new Span("⭐ Rating: " + item.getAverageRating())
            );

            // Reviews panel
            VerticalLayout reviews = new VerticalLayout();
            reviews.setVisible(false);
            reviews.getStyle().set("padding", "0.5rem");

            for (ItemReviewDTO r : item.getReviews()) {
                Span line = new Span("• [" + r.getRating() + "] " + r.getReviewText());
                reviews.add(line);
            }

            Button toggle = new Button("Show Reviews");
            toggle.addClickListener(ev -> {
                boolean vis = reviews.isVisible();
                reviews.setVisible(!vis);
                toggle.setText(vis ? "Show Reviews" : "Hide Reviews");
            });

            Button addRev = new Button("Add Review", ev -> openReviewDialog(item.getId(), item.getName()));
            if (Boolean.TRUE.equals((Boolean)VaadinSession.getCurrent().getAttribute("isSuspended"))) {
                addRev.setVisible(false);
            }

            card.add(toggle, addRev, reviews);
            itemsContainer.add(card);
        }
    }

    private void openReviewDialog(int itemId, String itemName) {
        Dialog dlg = new Dialog();
        dlg.setWidth("400px");

        VerticalLayout form = new VerticalLayout();
        form.add(new H1("Review “" + itemName + "”"));

        NumberField rating = new NumberField("Rating (1–5)");
        rating.setMin(1);
        rating.setMax(5);
        rating.setStep(1);
        rating.setWidthFull();

        TextField text = new TextField("Your Review");
        text.setWidthFull();

        Button submit = new Button("Submit", ev -> {
            sendReview(itemId, rating.getValue(), text.getValue(), dlg);
        });
        Button cancel = new Button("Cancel", ev -> dlg.close());

        form.add(rating, text, new HorizontalLayout(submit, cancel));
        dlg.add(form);
        dlg.open();
    }

    private void sendReview(int itemId, Double rating, String review, Dialog dlg) {
        if (rating == null || rating < 1 || rating > 5) {
            Notification.show("Rating must be 1–5"); return;
        }
        if (review == null || review.isBlank()) {
            Notification.show("Please enter text"); return;
        }
        String token = (String)VaadinSession.getCurrent().getAttribute("authToken");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        String url = apiBase
                   + "/items/" + itemId + "/reviews"
                   + "?token=" + token
                   + "&rating=" + rating.intValue()
                   + "&reviewText=" + review;

        rest.postForEntity(url, new HttpEntity<>(headers), Void.class);
        Notification.show("Review added!");
        dlg.close();
        fetchAllItems();
        filterAndDisplay("");
    }

    private void handleSuspence() {
        Integer u = (Integer)VaadinSession.getCurrent().getAttribute("userId");
        if (u == null) return;
        String token = (String)VaadinSession.getCurrent().getAttribute("authToken");
        String url = apiBase + "/users/" + u + "/isSuspended?token=" + token;
        Boolean suspended = rest.getForObject(url, Boolean.class);
        VaadinSession.getCurrent().setAttribute("isSuspended", suspended);
    }
}
