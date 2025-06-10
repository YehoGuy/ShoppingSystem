package UI;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import DTOs.ShopDTO;

@Route(value = "myshops", layout = AppLayoutBasic.class)

public class MyShopsView extends VerticalLayout implements BeforeEnterObserver {

    @Value("${url.api}/shops")
    private String BASE_URL;

    @Value("${url.api}/shops/ByWorkerId")
    private String GET_SHOPS;

    @Value("${url.api}/shops/create")
    private String CREATE;

    private final RestTemplate restTemplate = new RestTemplate();
    private List<ShopDTO> allShops;
    private VerticalLayout shopsContainer;
    private TextField searchField;

    public MyShopsView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Title
        add(new H1("🏠 My Shops"));

        // Search and Add button
        searchField = new TextField();
        searchField.setPlaceholder("🔍 Search shops...");
        searchField.setWidth("300px");
        searchField.addValueChangeListener(e -> filterAndDisplay());

        Button addBtn = new Button("➕ Add New Shop", e -> openCreateDialog());
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            addBtn.setVisible(false);
        }
        HorizontalLayout header = new HorizontalLayout(searchField, addBtn);
        header.setAlignItems(Alignment.CENTER);
        add(header);

        // Shops container
        shopsContainer = new VerticalLayout();
        shopsContainer.setSizeFull();
        shopsContainer.getStyle()
            .set("overflow", "auto")
            .set("padding", "30px")
            .set("gap", "0px")
            .set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)")
            .set("min-height", "100vh");
            
        add(shopsContainer);

    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // Ensure userId is set in session
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            event.forwardTo("login");
        }
        loadShops();

        handleSuspence();
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    private void loadShops() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        try {
            System.out.println("url: " + GET_SHOPS + "?workerId=" + getUserId() + "&token=" + token);
            ResponseEntity<ShopDTO[]> resp = restTemplate
                    .getForEntity(GET_SHOPS + "?workerId=" + getUserId() + "&token=" + token, ShopDTO[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                allShops = Arrays.asList(resp.getBody());
                filterAndDisplay();
            } else {
                Notification.show("⚠️ Failed to load shops");
            }
        } catch (Exception ex) {
            Notification.show("❗ Error loading shops");
        }
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);

        TextField nameField = new TextField("Shop Name");
        nameField.setWidth("100%");

        Button create = new Button("Create", e -> {
            String name = nameField.getValue().trim();
            if (!name.isEmpty()) {
                createShop(name);
                dialog.close();
            }
        });
        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(new VerticalLayout(nameField, new HorizontalLayout(create, cancel)));
        dialog.open();
    }

    private void createShop(String name) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        try {
            String url = CREATE + "?token=" + token + "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
            ResponseEntity<ShopDTO> resp = restTemplate.postForEntity(url, null, ShopDTO.class);

            if (resp.getStatusCode() == HttpStatus.CREATED) {
                Notification.show("✅ Shop created: " + name);
                loadShops();
            } else {
                Notification.show("⚠️ Could not create shop");
            }
        } catch (Exception ex) {
            Notification.show("❗ Error creating shop");
        }
    }

    private void filterAndDisplay() {
        String q = searchField.getValue();
        List<ShopDTO> filtered = allShops.stream()
                .filter(s -> s.getName().toLowerCase().contains(q.toLowerCase()))
                .toList();
        displayShops(filtered);
    }

    private void displayShops(List<ShopDTO> shops) {
        shopsContainer.removeAll();
        if (shops.isEmpty()) {
            shopsContainer.add(new Paragraph("No shops found."));
            return;
        }
        for (ShopDTO s : shops) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();

            // a container for the shop name with icon
            com.vaadin.flow.component.html.Div nameContainer = new com.vaadin.flow.component.html.Div();
            nameContainer.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("gap", "12px")
                    .set("cursor", "pointer")
                    .set("padding", "16px 20px")
                    .set("border-radius", "12px")
                    .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
                    .set("color", "white")
                    .set("box-shadow", "0 4px 15px rgba(102, 126, 234, 0.3)")
                    .set("transition", "all 0.3s ease")
                    .set("min-width", "300px");

            // Shop icon
            Span icon = new Span("🏪");
            icon.getStyle()
                    .set("font-size", "28px")
                    .set("filter", "drop-shadow(0 2px 4px rgba(0,0,0,0.3))");

            // Shop name text
            Span nameText = new Span(s.getName());
            nameText.getStyle()
                    .set("font-size", "22px")
                    .set("font-weight", "700")
                    .set("text-shadow", "0 1px 3px rgba(0,0,0,0.3)")
                    .set("letter-spacing", "0.5px");

            // Shop ID badge
            Span idBadge = new Span("ID: " + s.getShopId());
            idBadge.getStyle()
                    .set("background-color", "rgba(255,255,255,0.2)")
                    .set("padding", "4px 8px")
                    .set("border-radius", "20px")
                    .set("font-size", "12px")
                    .set("font-weight", "500")
                    .set("margin-left", "auto");

            nameContainer.add(icon, nameText, idBadge);

            nameContainer.addClickListener(evt -> {
                nameContainer.getStyle()
                        .set("transform", "translateY(-2px)")
                        .set("box-shadow", "0 6px 20px rgba(102, 126, 234, 0.4)");
                UI.getCurrent().navigate("shop/" + s.getShopId());
            });

            // Add hover effects
            nameContainer.getElement().addEventListener("mouseenter", e -> {
                nameContainer.getStyle()
                        .set("transform", "translateY(-2px)")
                        .set("box-shadow", "0 6px 20px rgba(102, 126, 234, 0.4)");
            });

            nameContainer.getElement().addEventListener("mouseleave", e -> {
                nameContainer.getStyle()
                        .set("transform", "translateY(0)")
                        .set("box-shadow", "0 4px 15px rgba(102, 126, 234, 0.3)");
            });

            Button view = new Button("✏️ Edit Shop", e -> UI.getCurrent().navigate("edit-shop/" + s.getShopId()));
            view.getStyle()
                    .set("background", "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)")
                    .set("color", "white")
                    .set("border", "none")
                    .set("border-radius", "12px")
                    .set("padding", "12px 20px")
                    .set("font-weight", "600")
                    .set("font-size", "14px")
                    .set("cursor", "pointer")
                    .set("box-shadow", "0 4px 15px rgba(240, 147, 251, 0.4)")
                    .set("transition", "all 0.3s ease")
                    .set("text-transform", "uppercase")
                    .set("letter-spacing", "0.5px");

            Button historyBtn = new Button("📊 Purchase History", e -> UI.getCurrent().navigate("history/" + s.getShopId()));
            historyBtn.getStyle()
                    .set("background", "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)")
                    .set("color", "white")
                    .set("border", "none")
                    .set("border-radius", "12px")
                    .set("padding", "12px 20px")
                    .set("font-weight", "600")
                    .set("font-size", "14px")
                    .set("cursor", "pointer")
                    .set("box-shadow", "0 4px 15px rgba(79, 172, 254, 0.4)")
                    .set("transition", "all 0.3s ease")
                    .set("text-transform", "uppercase")
                    .set("letter-spacing", "0.5px");

            // Add hover effects for buttons
            view.getElement().addEventListener("mouseenter", e -> {
                view.getStyle().set("transform", "translateY(-2px)")
                        .set("box-shadow", "0 6px 20px rgba(240, 147, 251, 0.6)");
            });
            view.getElement().addEventListener("mouseleave", e -> {
                view.getStyle().set("transform", "translateY(0)")
                        .set("box-shadow", "0 4px 15px rgba(240, 147, 251, 0.4)");
            });

            historyBtn.getElement().addEventListener("mouseenter", e -> {
                historyBtn.getStyle().set("transform", "translateY(-2px)")
                        .set("box-shadow", "0 6px 20px rgba(79, 172, 254, 0.6)");
            });
            historyBtn.getElement().addEventListener("mouseleave", e -> {
                historyBtn.getStyle().set("transform", "translateY(0)")
                        .set("box-shadow", "0 4px 15px rgba(79, 172, 254, 0.4)");
            });

            // Create a premium card-like container for each shop
            com.vaadin.flow.component.html.Div shopCard = new com.vaadin.flow.component.html.Div();
            shopCard.getStyle()
                    .set("background", "linear-gradient(145deg, #ffffff 0%, #f8fafc 100%)")
                    .set("border-radius", "20px")
                    .set("box-shadow", "0 8px 32px rgba(0, 0, 0, 0.12)")
                    .set("padding", "24px")
                    .set("margin-bottom", "24px")
                    .set("border", "1px solid rgba(255, 255, 255, 0.8)")
                    .set("backdrop-filter", "blur(10px)")
                    .set("transition", "all 0.4s ease")
                    .set("position", "relative")
                    .set("overflow", "hidden");

            // Add decorative gradient overlay
            com.vaadin.flow.component.html.Div overlay = new com.vaadin.flow.component.html.Div();
            overlay.getStyle()
                    .set("position", "absolute")
                    .set("top", "0")
                    .set("left", "0")
                    .set("right", "0")
                    .set("height", "4px")
                    .set("background", "linear-gradient(90deg, #667eea 0%, #764ba2 50%, #f093fb 100%)");

            VerticalLayout cardContent = new VerticalLayout();
            cardContent.setSpacing(true);
            cardContent.setPadding(false);

            HorizontalLayout buttonGroup = new HorizontalLayout(view, historyBtn);
            buttonGroup.setSpacing(true);
            buttonGroup.setJustifyContentMode(JustifyContentMode.END);

            cardContent.add(nameContainer, buttonGroup);
            shopCard.add(overlay, cardContent);

            // Add hover effect for the entire card
            shopCard.getElement().addEventListener("mouseenter", e -> {
                shopCard.getStyle()
                        .set("transform", "translateY(-4px)")
                        .set("box-shadow", "0 12px 40px rgba(0, 0, 0, 0.15)");
            });
            shopCard.getElement().addEventListener("mouseleave", e -> {
                shopCard.getStyle()
                        .set("transform", "translateY(0)")
                        .set("box-shadow", "0 8px 32px rgba(0, 0, 0, 0.12)");
            });

            shopsContainer.add(shopCard);
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
            Notification.show(
                    "Failed to check admin status");
        }
    }
}