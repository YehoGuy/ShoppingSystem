package UI;

import java.util.*;
import java.util.stream.Collectors;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;

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
                "shop A",
                Map.of(
                        new ItemDTO(1, "Banana", "Fresh yellow banana", 2.5, ItemCategory.GROCERY), 5,
                        new ItemDTO(2, "Apple", "Juicy red apple", 3.0, ItemCategory.GROCERY), 10
                ),
                Map.of(
                        new ItemDTO(1, "Banana", "Fresh yellow banana", 2.5, ItemCategory.GROCERY), 2,
                        new ItemDTO(2, "Apple", "Juicy red apple", 3.0, ItemCategory.GROCERY), 3
                ),
                List.of(
                        new ShopReviewDTO(1, 5, "Great service!", "shop A"),
                        new ShopReviewDTO(2, 4, "Good selection of products.", "shop A")
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

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        VerticalLayout titleSection = new VerticalLayout();
        titleSection.setAlignItems(Alignment.CENTER);
        
        H1 title = new H1("🛍️ Welcome to " + selectedShop.getName());
        title.getStyle().set("margin-bottom", "10px");
        
        Span avgRating = new Span("⭐ Average Rating: " + calculateAverageRating(selectedShop.getReviews()));
        
        titleSection.add(title, avgRating);

        Button reviewButton = new Button("Leave Review");
        reviewButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            @Override
            public void onComponentEvent(ClickEvent<Button> e) {
                String shopName = selectedShop.getName();
                Dialog reviewDialog = new Dialog();
                reviewDialog.setWidth("400px");
                
                VerticalLayout dialogLayout = new VerticalLayout();
                
                NumberField ratingField = new NumberField("Rating (1-5 stars)");
                ratingField.setMin(1);
                ratingField.setMax(5);
                ratingField.setStep(1);
                ratingField.setValue(5.0);
                ratingField.setStepButtonsVisible(true);
                
                TextArea messageField = new TextArea("Review Message");
                messageField.setWidth("100%");
                
                HorizontalLayout buttons = new HorizontalLayout();
                Button cancelButton = new Button("Cancel", event -> reviewDialog.close());
                Button sendButton = new Button("Send Review");
                sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                sendButton.addClickListener(event -> {
                    if (ratingField.getValue() == null || messageField.getValue().trim().isEmpty()) {
                        Notification.show("Please fill in all fields");
                        return;
                    }
                    // TODO: Add logic to save review
                    Notification.show("Review submitted successfully!");
                    reviewDialog.close();
                });
                
                buttons.add(cancelButton, sendButton);
                buttons.setJustifyContentMode(JustifyContentMode.END);
                
                dialogLayout.add(ratingField, messageField, buttons);
                reviewDialog.add(dialogLayout);
                
                reviewDialog.open();
            }
        });

        Button contactButton = new Button("Contact Seller");
        contactButton.addClickListener(e -> {
            Dialog contactDialog = new Dialog();
            contactDialog.setWidth("400px");
            
            VerticalLayout dialogLayout = new VerticalLayout();
            
            TextField titleField = new TextField("Title");
            titleField.setWidth("70%");
            TextArea messageField = new TextArea("Message to Seller");
            messageField.setWidth("100%");
            
            HorizontalLayout buttons = new HorizontalLayout();
            Button cancelButton = new Button("Cancel", event -> contactDialog.close());
            Button sendButton = new Button("Send Message");

            buttons.add(cancelButton, sendButton);

            dialogLayout.add(titleField, messageField, buttons);

            sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            sendButton.addClickListener(event -> {
                if (messageField.getValue().trim().isEmpty()) {
                    Notification.show("Please fill in the message field");
                    return;
                }
            });

            contactDialog.add(dialogLayout);
            contactDialog.open();
        });

        header.add(contactButton, titleSection, reviewButton);
        add(header);


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

            Button instaBuyButton = new Button("Buy imidiatly");
            instaBuyButton.getStyle().set("margin-top", "10px");
            instaBuyButton.addClickListener(e -> {
                Notification.show("Bought: " + item.getName());
            });

            Button addBidButton = new Button("Bid on item");
            addBidButton.getStyle().set("margin-top", "10px");
            addBidButton.addClickListener(e -> {
                Dialog bidDialog = new Dialog();
                bidDialog.setWidth("400px");
                
                VerticalLayout dialogLayout = new VerticalLayout();
                
                H3 title = new H3("Bid on " + item.getName());
                H5 minimum = new H5("Minimum Bid: $" + price);
                
                NumberField bidField = new NumberField("Bid Amount");
                bidField.setMin(0);
                bidField.setStep(0.1);
                
                HorizontalLayout buttons = new HorizontalLayout();
                Button cancelButton = new Button("Cancel", event -> bidDialog.close());
                Button sendButton = new Button("Place Bid");
                sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                sendButton.addClickListener(event -> {
                    if (bidField.getValue() == null) {
                        Notification.show("Please enter a valid bid amount");
                        return;
                    }
                });
                buttons.add(cancelButton, sendButton);
                buttons.setJustifyContentMode(JustifyContentMode.END);
                dialogLayout.add(title, minimum, bidField, buttons);
                bidDialog.add(dialogLayout);
                bidDialog.open();
            });

            itemCard.add(name, desc, priceSpan, new HorizontalLayout(addCartButton, instaBuyButton, addBidButton));
            itemsContainer.add(itemCard);
        }
    }

    private double calculateAverageRating(List<ShopReviewDTO> reviews) {
        if (reviews == null || reviews.isEmpty()) return 0.0;
        return reviews.stream().mapToDouble(ShopReviewDTO::getRating).average().orElse(0.0);
    }
}
