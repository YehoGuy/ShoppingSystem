package UI;

import DTOs.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.*;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "shop", layout = AppLayoutBasic.class)
@JsModule("@vaadin/dialog/vaadin-dialog.js")
@JsModule("@vaadin/number-field/vaadin-number-field.js")
public class ShopView extends BaseView
    implements HasUrlParameter<String>, BeforeEnterObserver {

    private final String api;
    private final String shopApiUrl;
    private final String usersUrl;
    private final String purchaseHistoryUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private ShopDTO shop;
    private Map<ItemDTO, Double> prices;
    private List<MemberDTO> memberDTOs = new ArrayList<>();

    public ShopView(@Value("${url.api}") String api) {
        super("Shop Details", "Your storefront", "🏬", "➡️");
        this.api                 = api;
        this.shopApiUrl          = api + "/shops";
        this.usersUrl            = api + "/users";
        this.purchaseHistoryUrl  = api + "/purchases/shops";
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // ensure userId
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
            return;
        }
        handleSuspence();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String shopId) {
        if (shopId == null || shopId.isEmpty()) {
            add(new Span("❌ No shop ID provided."));
            return;
        }
        String token = getToken();
        String url = shopApiUrl + "/" + shopId + "?token=" + token;
        try {
            ResponseEntity<ShopDTO> resp = restTemplate.getForEntity(url, ShopDTO.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                shop = resp.getBody();
                prices = ShopDTO.itemPricesToMapConverter(
                    shop.getItems(), shop.getItemPrices()
                );
                buildPage();
            } else {
                Notification.show("⚠️ Failed to load shop");
            }
        } catch (Exception e) {
            Notification.show("❗ Error loading shop");
        }
    }

    private void buildPage() {
        removeAll();
        loadUsers();

        // wrap everything in a styled card
        VerticalLayout card = new VerticalLayout();
        card.addClassName("view-card");
        card.setSizeFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
            .set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)");

        // Header
        H1 header = new H1("🛍️ Welcome to " + shop.getName());
        card.add(header);

        // Items section
        card.add(new H2("📦 Items"));
        VerticalLayout itemsLayout = new VerticalLayout();
        itemsLayout.setWidthFull();
        ShopDTO.itemQuantitiesToMapConverter(
            shop.getItems(), shop.getItemQuantities()
        ).forEach((item, available) -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.getStyle().set("align-items", "center");

            Span name       = new Span("🍽️ " + item.getName());
            Span priceSpan  = new Span("💲 " + prices.getOrDefault(item, 0.0));
            Span stock      = new Span("📊 In Stock: " + available);

            Button bidButton = new Button("Create Bid", ev ->
                UI.getCurrent().navigate(
                    "shop/" + shop.getShopId() + "/create-bid/" + item.getId()
                )
            );

            IntegerField qtyField = new IntegerField();
            qtyField.setLabel("Quantity");
            qtyField.setValue(1);
            qtyField.setMin(1);
            qtyField.setMax(available);
            qtyField.setStepButtonsVisible(true);
            qtyField.setWidth("80px");

            Button addBtn = new Button("🛒 Add to Cart", ev -> {
                int qty = qtyField.getValue() != null ? qtyField.getValue() : 1;
                if (qty > available) {
                    Notification.show("❌ Only " + available + " in stock");
                    return;
                }
                String cartUrl = api
                    + "/users/shoppingCart/"
                    + shop.getShopId()
                    + "/" + item.getId()
                    + "?quantity=" + qty
                    + "&token=" + getToken();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Void> req = new HttpEntity<>(headers);
                try {
                    ResponseEntity<Void> resp = restTemplate.exchange(
                        cartUrl, HttpMethod.POST, req, Void.class
                    );
                    if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
                        Notification.show("🚀 Added “" + item.getName() + "” x" + qty + " to cart");
                    } else {
                        Notification.show("❌ Could not add to cart: " + resp.getStatusCode());
                    }
                } catch (Exception ex) {
                    Notification.show("❌ Error adding to cart: " + ex.getMessage());
                }
            });

            row.add(name, priceSpan, stock, bidButton, qtyField, addBtn);
            itemsLayout.add(row);
        });
        card.add(itemsLayout);

        // Bids section
        card.add(new H2("📢 Bids for This Shop"));
        Map<Integer,String> itemNames = shop.getItems().stream()
            .collect(Collectors.toMap(ItemDTO::getId, ItemDTO::getName));

        Grid<BidRecieptDTO> shopBidsGrid = new Grid<>(BidRecieptDTO.class, false);
        shopBidsGrid.addColumn(dto ->
            itemNames.getOrDefault(
                dto.getItems().keySet().stream().findFirst().orElse(-1), "")
        ).setHeader("Item Name").setAutoWidth(true);

        shopBidsGrid.addColumn(dto -> matchUserName(dto.getUserId()))
            .setHeader("Owner Bid Name").setAutoWidth(true);
        shopBidsGrid.addColumn(BidRecieptDTO::getInitialPrice)
            .setHeader("Initial Price").setAutoWidth(true);
        shopBidsGrid.addColumn(BidRecieptDTO::getHighestBid)
            .setHeader("Highest Bid").setAutoWidth(true);
        shopBidsGrid.addColumn(dto -> dto.isCompleted() ? "Yes" : "No")
            .setHeader("Completed").setAutoWidth(true);

        shopBidsGrid.addColumn(new ComponentRenderer<>(dto -> {
            Span timer = new Span();
            Runnable update = () -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime end = dto.getEndTime();
                if (end == null) {
                    timer.setText("—");
                } else if (end.isBefore(now)) {
                    timer.setText("Ended");
                } else {
                    Duration d = Duration.between(now, end);
                    timer.setText(String.format(
                        "%02d:%02d:%02d",
                        d.toHours(),
                        d.toMinutesPart(),
                        d.toSecondsPart()
                    ));
                }
            };
            update.run();
            UI ui = UI.getCurrent();
            ui.setPollInterval(1000);
            ui.addPollListener(e -> update.run());
            return timer;
        })).setHeader("Time Left").setAutoWidth(true);

        shopBidsGrid.addColumn(new ComponentRenderer<>(dto -> {
            if (dto.getEndTime() != null) {
                Button btn = new Button("Add Offer", e -> {
                    Integer me = getUserId();
                    if (me != null && me.equals(dto.getUserId())) {
                        Notification.show(
                            "You cannot place a bid on your own auction",
                            3000, Position.MIDDLE
                        );
                    } else {
                        UI.getCurrent().navigate("auction/" + dto.getPurchaseId());
                    }
                });
                return btn;
            } else {
                return new Span();
            }
        })).setHeader("Auction").setAutoWidth(true);

        shopBidsGrid.setAllRowsVisible(true);
        fetchStoreBids(shopBidsGrid);
        shopBidsGrid.asSingleSelect().addValueChangeListener(ev -> {
            BidRecieptDTO sel = ev.getValue();
            if (sel != null) {
                UI.getCurrent().navigate("bid/" + sel.getPurchaseId());
            }
        });

        card.add(shopBidsGrid);
        card.expand(shopBidsGrid);
        add(card);
        expand(card);

        // Reviews section
        card.add(new H2("📝 Reviews"));
        double avg = shop.getReviews().stream()
                        .mapToInt(ShopReviewDTO::getRating)
                        .average().orElse(0.0);
        card.add(new Paragraph("⭐ Average Rating: " +
            String.format("%.1f", avg) + "/5"));

        for (ShopReviewDTO rev : shop.getReviews()) {
            card.add(new Paragraph(
                matchUserName(rev.getUserId())
                + ": " + rev.getReviewText()
                + " (" + rev.getRating() + ")"
            ));
        }

        Button addReviewButton = new Button("Add Review", e ->
            addReview(shop.getShopId())
        );
        if (Boolean.TRUE.equals(
            VaadinSession.getCurrent().getAttribute("isSuspended")))
        {
            addReviewButton.setVisible(false);
        }
        card.add(addReviewButton);

        // add and expand the card
        add(card);
        expand(card);
    }

    private void fetchStoreBids(Grid<BidRecieptDTO> shopBidsGrid) {
        try {
            String authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");
            if (authToken == null || authToken.isBlank()) {
                shopBidsGrid.setItems(Collections.emptyList());
                return;
            }
            String url = purchaseHistoryUrl
                + "/" + shop.getShopId() + "/bids?authToken=" + authToken;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<BidRecieptDTO>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                shopBidsGrid.setItems(response.getBody());
            } else {
                add(new H2("Failed to load shop’s bids"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            add(new H2("Error fetching shop’s bids"));
        }
    }

    private void loadUsers() {
        try {
            String token = getToken();
            HttpEntity<Void> req = new HttpEntity<>(getHeaders(token));
            String url = usersUrl + "/allmembers?token=" + token;
            ResponseEntity<MemberDTO[]> resp = restTemplate.exchange(
                url, HttpMethod.GET, req, MemberDTO[].class
            );
            memberDTOs = resp.getBody() != null
                ? Arrays.asList(resp.getBody())
                : Collections.emptyList();
        } catch (Exception e) {
            Notification.show("Failed to load users");
        }
    }

    private String matchUserName(int userId) {
        return memberDTOs.stream()
            .filter(m -> m.getMemberId() == userId)
            .map(MemberDTO::getUsername)
            .findFirst()
            .orElse("Unknown User");
    }

    private void addReview(int shopId) {
        Dialog dialog = new Dialog();
        dialog.add(new H1("Add Review to " + shop.getName()));

        NumberField ratingField = new NumberField("Rating (1-5)");
        ratingField.setMin(1);
        ratingField.setMax(5);
        ratingField.setStep(1);

        TextField reviewText = new TextField("Your Review");
        reviewText.setWidthFull();

        Button submit = new Button("Submit Review", ev ->
            sendReview(shopId, reviewText.getValue(), ratingField.getValue(), dialog)
        );
        Button close = new Button("Close", ev -> dialog.close());

        dialog.add(new VerticalLayout(ratingField, reviewText, submit, close));
        dialog.open();
    }

    private void sendReview(int shopId, String text, Double dblRating, Dialog dialog) {
        if (text == null || text.trim().isEmpty()) {
            Notification.show("Please enter your review text.");
            return;
        }
        if (dblRating == null || dblRating < 1 || dblRating > 5) {
            Notification.show("Please enter a valid rating between 1 and 5.");
            return;
        }
        int rating = dblRating.intValue();
        String token = getToken();
        HttpHeaders headers = getHeaders(token);
        HttpEntity<Void> req = new HttpEntity<>(headers);
        String url = shopApiUrl + "/" + shop.getShopId() + "/reviews"
                   + "?token=" + token
                   + "&rating=" + rating
                   + "&reviewText=" + text;
        try {
            restTemplate.postForEntity(url, req, Void.class);
            Notification.show("Review added successfully");
            dialog.close();
            setParameter(null, String.valueOf(shop.getShopId()));
        } catch (Exception e) {
            Notification.show("Error adding review: " + e.getMessage());
        }
    }

    private Integer getUserId() {
        Object uid = VaadinSession.getCurrent().getAttribute("userId");
        if (uid != null) {
            return Integer.parseInt(uid.toString());
        }
        UI.getCurrent().navigate("");
        return null;
    }

    private String getToken() {
        return (String) VaadinSession.getCurrent().getAttribute("authToken");
    }

    private HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    private void handleSuspence() {
        Integer userId = getUserId();
        String token = getToken();
        if (userId == null || token == null) return;

        String url = api + "/users/" + userId + "/isSuspended?token=" + token;
        try {
            ResponseEntity<Boolean> resp = restTemplate.getForEntity(url, Boolean.class);
            VaadinSession.getCurrent()
                .setAttribute("isSuspended",
                              resp.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(resp.getBody()));
        } catch (Exception e) {
            Notification.show("Failed to check suspension status");
        }
    }
}
