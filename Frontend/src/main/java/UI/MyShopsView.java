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
import com.vaadin.flow.component.html.Div;
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

    private final String api;
    private final String getShopsUrl;
    private final String createUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private List<ShopDTO> allShops;
    private VerticalLayout shopsContainer;
    private TextField searchField;

    public MyShopsView(@Value("${url.api}") String api) {
        this.api           = api;
        this.getShopsUrl   = api + "/shops/ByWorkerId";
        this.createUrl     = api + "/shops/create";

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Title
        // Create enhanced title container
        Div titleContainer = new Div();
        titleContainer.getStyle()
            // smooth two-tone diagonal gradient
            .set("background", "linear-gradient(135deg, #7789d9 0%, #9166bd 100%)")
            .set("padding", "50px")
            .set("border-radius", "24px")
            .set("box-shadow", "0 12px 40px rgba(0, 0, 0, 0.25)")
            .set("color", "white")
            .set("text-align", "center")
            .set("position", "relative")
            .set("overflow", "hidden");

        // decorative blur circles
        Div circle1 = new Div();
        circle1.getStyle()
            .set("position", "absolute")
            .set("width", "180px").set("height", "180px")
            .set("background", "rgba(255,255,255,0.10)")
            .set("border-radius", "50%")
            .set("top", "-40px").set("left", "-40px")
            .set("filter", "blur(60px)");

        Div circle2 = new Div();
        circle2.getStyle()
            .set("position", "absolute")
            .set("width", "260px").set("height", "260px")
            .set("background", "rgba(255,255,255,0.08)")
            .set("bottom", "-60px").set("right", "-80px")
            .set("filter", "blur(80px)");

        // add circles behind the text
        titleContainer.add(circle1, circle2);


        // Add animated gradient overlay
        com.vaadin.flow.component.html.Div titleOverlay = new com.vaadin.flow.component.html.Div();
        titleOverlay.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("right", "0")
                .set("height", "5px")
                .set("background", "linear-gradient(90deg, #f093fb 0%, #f5576c 25%, #4facfe 50%, #00f2fe 75%, #f093fb 100%)")
                .set("background-size", "200% 100%")
                .set("animation", "gradientShift 3s ease-in-out infinite");

        // Create the main title area: 
        Div titleContent = new Div();
        titleContent.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")    
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("gap", "8px");                 

        // (1) Subtitle
        Span subtitle = new Span("Manage Your Business Empire");
        subtitle.getStyle()
            .set("font-size", "16px")
            .set("font-weight", "700")
            .set("opacity", "0.9")
            .set("margin", "0");

        // (2) Row containing icon + heading + icon
        Div titleRow = new Div();
        titleRow.getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("gap", "16px");

        Span titleIcon = new Span("🧮");
        titleIcon.getStyle()
            .set("font-size", "48px")
            .set("filter", "drop-shadow(0 4px 8px rgba(0,0,0,0.3))")
            .set("animation", "bounce 2s ease-in-out infinite");

        H1 title = new H1("Shop Dashboard");
        title.getStyle()
            .set("margin", "0")
            .set("font-size", "42px")
            .set("font-weight", "900")
            .set("text-shadow", "0 3px 10px rgba(0,0,0,0.4)")
            .set("letter-spacing", "2px");

        Span shopIcon = new Span("🏛️");
        shopIcon.getStyle()
            .set("font-size", "48px")
            .set("filter", "drop-shadow(0 4px 8px rgba(0,0,0,0.3))")
            .set("animation", "bounce 2s ease-in-out infinite");

        titleRow.add(titleIcon, title, shopIcon);

        // (3) assemble into the container
        titleContent.add(titleRow, subtitle);
        titleContainer.add(titleOverlay, titleContent);

        // Add CSS animations
        getElement().executeJs(
            "const style = document.createElement('style');" +
            "style.textContent = `\n" +
            "@keyframes gradientShift { 0%,100% { background-position: 0% 50%; } 50% { background-position: 100% 50%; } }\n" +
            "@keyframes bounce        { 0%,20%,50%,80%,100% { transform: translateY(0); } 40% { transform: translateY(-10px); } 60% { transform: translateY(-5px); } }\n" +
            "@keyframes bgShift       { 0% { background-position: 0% 50%; } 50% { background-position: 100% 50%; } 100% { background-position: 0% 50%; } }\n" +
            "`;" +
            "document.head.appendChild(style);"
        );

        add(titleContainer);

        // Search and Add button
        searchField = new TextField();
        searchField.setPlaceholder("🔍 Search your shops...");
        searchField.setWidth("400px");
        searchField.addValueChangeListener(e -> filterAndDisplay());

        // Style the search field
        searchField.getStyle()
            .set("background", "#ffffff")
            .set("border", "2px solid #e2e8f0")
            .set("border-radius", "16px")
            .set("padding", "0")
            .set("box-shadow", "0 8px 25px rgba(0, 0, 0, 0.1)")
            .set("transition", "all 0.3s ease");

        // Style the input element directly
        searchField.getElement().executeJs(
            "this.inputElement.style.padding = '16px 20px';" +
            "this.inputElement.style.fontSize = '16px';" +
            "this.inputElement.style.fontWeight = '500';" +
            "this.inputElement.style.border = 'none';" +
            "this.inputElement.style.outline = 'none';" +
            "this.inputElement.style.background = 'transparent';" +
            "this.inputElement.style.color = '#2d3748';" +
            "this.inputElement.addEventListener('focus', () => {" +
            "  this.parentElement.style.borderImage = 'linear-gradient(135deg, #667eea, #764ba2) 1';" +
            "  this.parentElement.style.transform = 'translateY(-2px)';" +
            "  this.parentElement.style.boxShadow = '0 12px 35px rgba(102, 126, 234, 0.2)';" +
            "});" +
            "this.inputElement.addEventListener('blur', () => {" +
            "  this.parentElement.style.borderImage = 'none';" +
            "  this.parentElement.style.transform = 'translateY(0)';" +
            "  this.parentElement.style.boxShadow = '0 8px 25px rgba(0, 0, 0, 0.1)';" +
            "});"
        );

        Button addBtn = new Button("✨ Create New Shop", e -> openCreateDialog());
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            addBtn.setVisible(false);
        }

        // Style the add button with premium design
        addBtn.getStyle()
                .set("background", "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)")
                .set("color", "white")
                .set("border", "none")
                .set("border-radius", "16px")
                .set("padding", "16px 32px")
                .set("font-weight", "700")
                .set("font-size", "16px")
                .set("cursor", "pointer")
                .set("box-shadow", "0 8px 25px rgba(240, 147, 251, 0.4)")
                .set("transition", "all 0.3s ease")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "1px")
                .set("position", "relative")
                .set("overflow", "hidden");

        // Add hover and click effects
        addBtn.getElement().addEventListener("mouseenter", e -> {
            addBtn.getStyle()
                    .set("transform", "translateY(-3px)")
                    .set("box-shadow", "0 12px 35px rgba(240, 147, 251, 0.6)")
                    .set("background", "linear-gradient(135deg, #f5576c 0%, #f093fb 100%)");
        });

        addBtn.getElement().addEventListener("mouseleave", e -> {
            addBtn.getStyle()
                    .set("transform", "translateY(0)")
                    .set("box-shadow", "0 8px 25px rgba(240, 147, 251, 0.4)")
                    .set("background", "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)");
        });

        addBtn.getElement().addEventListener("mousedown", e -> {
            addBtn.getStyle().set("transform", "translateY(1px) scale(0.98)");
        });

        addBtn.getElement().addEventListener("mouseup", e -> {
            addBtn.getStyle().set("transform", "translateY(-3px) scale(1)");
        });
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
            
            ResponseEntity<ShopDTO[]> resp = restTemplate
                .getForEntity(getShopsUrl + "?workerId=" + getUserId() + "&token=" + token,
                  ShopDTO[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                allShops = Arrays.asList(resp.getBody());
                filterAndDisplay();
            } else {
                Notification.show("⚠️ Failed to load shops");
            }
        } catch (Exception ex) {
            Notification.show("❗ Error loading shop" + ex.getMessage());
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
            String url = createUrl + "?token=" + token
                + "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
                
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
            // Shop icon
            Span icon = new Span("🏢");
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
                .set("background", "#ffffff")
                .set("border-radius", "20px")
                .set("box-shadow", "0 8px 32px rgba(0, 0, 0, 0.12)")
                .set("padding", "24px")
                .set("margin-bottom", "24px")
                .set("border", "1px solid #e2e8f0")
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
        String url = api + "/users/"
            + userId + "/isSuspended?token=" + token;

        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }
}