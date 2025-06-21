package UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import DTOs.BidRecieptDTO; // for Map.Entry
import DTOs.ItemDTO; // if you use List elsewhere
import DTOs.ItemReviewDTO;
import DTOs.MemberDTO;
import DTOs.ShopDTO;
import DTOs.ShopReviewDTO;

@Route(value = "shop", layout = AppLayoutBasic.class)
@JsModule("@vaadin/dialog/vaadin-dialog.js")
// ⬇️ Add this line so NumberField appears in the client
@JsModule("@vaadin/number-field/vaadin-number-field.js")
public class ShopView extends VerticalLayout implements HasUrlParameter<String>, BeforeEnterObserver {

    private final String api;
    private final String shopApiUrl;
    private final String purchaseHistoryUrl;
    private final String usersUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private ShopDTO shop;
    private Map<ItemDTO, Double> prices;
    private List<MemberDTO> memberDTOs = new ArrayList<>();


    public ShopView(@Value("${url.api}") String api) {
        this.api = api;
        this.shopApiUrl = api + "/shops";
        this.usersUrl = api + "/users";
        this.purchaseHistoryUrl = api + "/purchases/shops";

        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // Ensure userId is set in session
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }

        handleSuspence();
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    @Override
    public void setParameter(com.vaadin.flow.router.BeforeEvent event, @OptionalParameter String shopId) {
        if (shopId == null || shopId.isEmpty()) {
            add(new Span("❌ No shop ID provided."));
            return;
        }

        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        String url = shopApiUrl + "/" + shopId + "?token=" + token;
        try {
            ResponseEntity<ShopDTO> resp = restTemplate.getForEntity(url, ShopDTO.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                shop = resp.getBody();
                prices = ShopDTO.itemPricesToMapConverter(shop.getItems(), shop.getItemPrices());
                buildPage();
            } else {
                Notification.show("⚠️ Failed to load shop");
            }
        } catch (Exception e) {
            Notification.show("❗ Error loading shop");
        }
    }

    private void fetchStoreBids(Grid<BidRecieptDTO> shopBidsGrid) {
        try {
            // 1. Read the authToken from VaadinSession
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            if (authToken == null || authToken.isBlank()) {
                // If there's no token, do not call the endpoint; just show an empty grid
                shopBidsGrid.setItems(Collections.emptyList());
                return;
            }

            // 2. Build the full URL including the required query param "authToken"
            //
            // EXACTLY matches the backend mapping:
            // @GetMapping("/shops/{shopId}/bids")
            // public ResponseEntity<List<BidRecieptDTO>> getBidsForShop(
            // @PathVariable int shopId,
            // @RequestParam String authToken)
            //
            // Therefore we must call:
            // GET /api/purchases/shops/{shopId}/bids?authToken=<token>
            //
            String url = purchaseHistoryUrl + "/" + shop.getShopId() + "/bids?authToken=" + authToken;

            // 3. Prepare headers (JSON)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 4. Make the GET call
            ResponseEntity<List<BidRecieptDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });

            // 5. If 200 OK, bind the response body (List<BidRecieptDTO>) to the grid
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                shopBidsGrid.setItems(response.getBody());
            } else {
                // If we get a 4xx or 5xx, show an error header
                add(new H2("Failed to load shop’s bids"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            add(new H2("Error fetching shop’s bids"));
        }
    }

    /**
     * Renders the shop page with header, items, and reviews.
     */
    private void buildPage() {
        removeAll();
        loadUsers(); // Load users to match usernames in reviews
        setPadding(true);
        setSpacing(true);

        // Header with emoji
        H1 header = new H1("🛍️ Welcome to " + shop.getName());
        add(header);

        // Items section
        add(new H2("📦 Items"));
        VerticalLayout itemsLayout = new VerticalLayout();
        itemsLayout.setWidthFull();

        for (Map.Entry<ItemDTO, Integer> e : ShopDTO.itemQuantitiesToMapConverter(
                shop.getItems(),
                shop.getItemQuantities()).entrySet()) {

            ItemDTO item = e.getKey();
            int available = e.getValue();

            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);
            row.getStyle().set("align-items", "center");

            Span name = new Span("🍽️ " + item.getName());
            Span priceSpan = new Span("💲 " + prices.getOrDefault(item, 0.0));
            Span stock = new Span("📊 In Stock: " + available);

            Button bidButton = new Button("Create Bid", click -> {
            // navigates to your CreateBidView for this shop
            UI.getCurrent().navigate("shop/" + shop.getShopId() + "/create-bid/" + item.getId());
            });

            row.add(name, priceSpan, stock, bidButton /* plus your other controls if any */);
            itemsLayout.add(row);

            // IntegerField to choose quantity
            IntegerField qtyField = new IntegerField();
            qtyField.setLabel("Quantity");
            qtyField.setValue(1);
            qtyField.setMin(1);
            qtyField.setMax(available);
            qtyField.setStepButtonsVisible(true);
            qtyField.setWidth("80px");

            // “Add to Cart” button with chosen quantity
            Button addBtn = new Button("🛒 Add to Cart", evt -> {
                // Read chosen quantity (default to 1 if null or invalid)
                Integer chosenQty = qtyField.getValue();
                int qty = (chosenQty != null && chosenQty > 0) ? chosenQty : 1;

                // Ensure it does not exceed available stock
                if (qty > available) {
                    Notification.show("❌ Only " + available + " in stock");
                    return;
                }

                // Check for auth token in VaadinSession
                String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
                if (authToken == null || authToken.isBlank()) {
                    Notification.show("❌ Please log in first");
                    return;
                }

                // Build URL: POST
                // http://localhost:8080/shops/{shopId}/cart/add?itemId={itemId}&quantity={qty}&token={authToken}
                String url = "http://localhost:8080/api/users/"
                        + "/shoppingCart/"
                        + shop.getShopId()
                        + "/"
                        + item.getId()
                        + "?quantity=" + qty
                        + "&token=" + authToken;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Void> request = new HttpEntity<>(headers);

                try {
                    ResponseEntity<Void> resp = restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            request,
                            Void.class);

                    if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
                        Notification.show("🚀 Added “" + item.getName() + "” x" + qty + " to cart");
                    } else {
                        Notification.show("❌ Could not add to cart: " + resp.getStatusCode());
                    }
                } catch (Exception ex) {
                    Notification.show("❌ Error adding to cart: " + ex.getMessage());
                }
            });

            row.add(name, priceSpan, stock, qtyField, addBtn);
            itemsLayout.add(row);
        }

        add(itemsLayout);

        // Bids section
        H2 bidsHeader = new H2("📢 Bids for This Shop");
        add(bidsHeader);
        Grid<BidRecieptDTO> shopBidsGrid = new Grid<>(BidRecieptDTO.class, false);
        shopBidsGrid.addColumn(BidRecieptDTO::getPurchaseId)
                .setHeader("Bid ID")
                .setAutoWidth(true);
        shopBidsGrid.addColumn(dto -> dto.getInitialPrice())
                .setHeader("Initial Price")
                .setAutoWidth(true);
        shopBidsGrid.addColumn(dto -> dto.getHighestBid())
                .setHeader("Highest Bid")
                .setAutoWidth(true);
        shopBidsGrid.addColumn(dto -> dto.isCompleted() ? "Yes" : "No")
                .setHeader("Completed");

        // When the user clicks a bid row, navigate to /bid/{purchaseId}
        shopBidsGrid.asSingleSelect().addValueChangeListener(event -> {
            BidRecieptDTO selected = event.getValue();
            if (selected != null) {
                UI.getCurrent().navigate("bid/" + selected.getPurchaseId());
            }
        });

        add(shopBidsGrid);

        // Reviews section
        add(new H2("📝 Reviews"));
        double avg = shop.getReviews().stream()
                .mapToInt(ShopReviewDTO::getRating)
                .average()
                .orElse(0.0);
        add(new Paragraph("⭐ Average Rating: " + String.format("%.1f", avg) + "/5"));
        for (ShopReviewDTO rev : shop.getReviews()) {
            add(new Paragraph("👤 " + matchUserName(rev.getUserId()) + ": " 
                    + rev.getReviewText() + " (" + rev.getRating() + ")"));
        }

        Button addReviewButton = new Button("Add Review");
        addReviewButton.addClickListener(event -> addReview(shop.getShopId()));
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            addReviewButton.setVisible(false);
        }

        add(addReviewButton);
        displayReviews(); // Call to display reviews

        // reviewContainer = new VerticalLayout(); // <--- set reference
        // reviewContainer.setWidth("80%");
        // reviewContainer.getStyle().set("overflow", "auto");
        // HorizontalLayout content = new HorizontalLayout(reviewContainer);
        // content.setWidthFull();
        // content.setHeightFull();
        // content.setFlexGrow(1, reviewContainer);
        // add(content);
        // displayReviews();
    }

    private void displayReviews() {

        VerticalLayout reviewsLayout = new VerticalLayout();

        for (ShopReviewDTO review : shop.getReviews()) {
            VerticalLayout singleReview = new VerticalLayout();
            singleReview.add(
                    new Span("Rating: " + review.getRating()),
                    new Span("Comment: " + review.getReviewText()));
            singleReview.getStyle().set("border", "1px solid #ccc");
            singleReview.getStyle().set("padding", "10px");
            singleReview.getStyle().set("margin-bottom", "10px");
            reviewsLayout.add(singleReview);
        }
        add(reviewsLayout);
    }

    private void getShopReviews() {
        // This method is a placeholder for fetching shop reviews.
        // You can implement it to fetch reviews from the backend or any other source.
        // Currently, it does nothing.
    }

    private void addReview(int shopId) {
        Dialog dialog = new Dialog();

        String shopName = (shop != null) ? shop.getName() : ("ID " + shopId);
        H1 dialogTitle = new H1("Add Review to " + shopName);
        dialog.add(dialogTitle);
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSpacing(true);

        NumberField ratingField = new NumberField("Rating (1-5)");
        // Alternatively, use a Slider for rating selection:
        ratingField.setMin(1);
        ratingField.setMax(5);
        ratingField.setStep(1);
        ratingField.setWidthFull();
        // ratingField.getStyle().set("border", "2px solid red");
        ratingField.setVisible(true);
        // ratingField.setHeight("50px"); // just to see it

        TextField reviewTextField = new TextField("Your Review");
        reviewTextField.setWidthFull();

        Button submitButton = new Button("Submit Review");
        submitButton
                .addClickListener(e -> sendReview(shopId, reviewTextField.getValue(), ratingField.getValue(), dialog));

        Button closeButton = new Button("Close", event -> dialog.close());

        formLayout.add(ratingField, reviewTextField, submitButton, closeButton);
        dialog.add(formLayout);

        // UI.getCurrent().add(dialog);
        dialog.open();

    }

    private void sendReview(int shopId, String reviewText, double double_rating, Dialog dialog) {
        try {
            if (reviewText == null || reviewText.trim().isEmpty()) {
                Notification.show("Please enter your review text.");
                return;
            }

            if (double_rating != 1.0 && double_rating != 2.0 && double_rating != 3.0 && double_rating != 4.0
                    && double_rating != 5.0) {
                Notification.show("Please enter a valid rating between 1 and 5.");
                return;
            }

            int rating = ((int) double_rating); // Convert double to int

            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = "http://localhost:8080" + "/api/shops/" + shopId + "/reviews"
                    + "?token=" + token + "&rating=" + rating + "&reviewText=" + reviewText + "&shopId=" + shopId;

            restTemplate.postForEntity(url, request, Void.class);
            Notification.show("review added successfully");

            getShopRefresh(shopId); // Refresh the displayed items

            dialog.close();

        } catch (Exception e) {
            Notification.show("something failed");
        }
    }

    private void getShopRefresh(int shopId) {
        String token = getToken();
        String url = shopApiUrl + "/" + shopId + "?token=" + token;
        try {
            ResponseEntity<ShopDTO> resp = restTemplate.getForEntity(url, ShopDTO.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                shop = resp.getBody();
                prices = ShopDTO.itemPricesToMapConverter(shop.getItems(), shop.getItemPrices());
                buildPage();
            } else {
                Notification.show("⚠️ Failed to load shop");
            }
        } catch (Exception e) {
            Notification.show("❗ Error loading shop");
        }
    }

    private String getToken() {
        return (String) VaadinSession.getCurrent().getAttribute("authToken");
    }

    private HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    private void loadUsers() {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = usersUrl + "/allmembers?token=" + token;

            ResponseEntity<MemberDTO[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, MemberDTO[].class);

            memberDTOs = response.getBody() != null ? Arrays.asList(response.getBody())
                    : Collections.emptyList();

            

        } catch (Exception e) {
            Notification.show("Failed to load users");
        }
    }

    private String matchUserName(int userId) {
        for (MemberDTO member : memberDTOs) {
            if (member.getMemberId() == userId) {
                return member.getUsername(); 
            }
        }
        return "Unknown Item";
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
        String url = "http://localhost:8080/api/users" + "/" + userId + "/isSuspended?token=" + token;
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }

}
