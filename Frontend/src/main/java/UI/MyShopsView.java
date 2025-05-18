package UI;

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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Route(value = "myshops", layout = AppLayoutBasic.class)
public class MyShopsView extends VerticalLayout implements BeforeEnterObserver {

    private static final String BASE_URL = "http://localhost:8080/api/shops";
    private static final String GET_ALL = BASE_URL + "/all";
    private static final String CREATE  = BASE_URL + "/create";

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
        HorizontalLayout header = new HorizontalLayout(searchField, addBtn);
        header.setAlignItems(Alignment.CENTER);
        add(header);

        // Shops container
        shopsContainer = new VerticalLayout();
        shopsContainer.setSizeFull();
        shopsContainer.getStyle()
            .set("overflow", "auto")
            .set("padding", "10px")
            .set("gap", "10px");
        add(shopsContainer);

        // Initial load
        loadShops();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            event.forwardTo("login");
        }
    }

    private void loadShops() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        try {
            ResponseEntity<ShopDTO[]> resp = restTemplate.getForEntity(GET_ALL + "?token=" + token, ShopDTO[].class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                allShops = Arrays.asList(resp.getBody());
                filterAndDisplay();
            } else {
                Notification.show("⚠️ Failed to load shops: " + resp.getStatusCode());
            }
        } catch (Exception ex) {
            Notification.show("❗ Error loading shops: " + ex.getMessage());
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
                Notification.show("⚠️ Could not create shop: " + resp.getStatusCode());
            }
        } catch (Exception ex) {
            Notification.show("❗ Error creating shop: " + ex.getMessage());
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
            
            Span name = new Span("🏷️ " + s.getName());
            name.getStyle()
                .set("cursor", "pointer")
                .set("font-weight", "600")
                .set("font-size", "18px");
            name.addClickListener(evt -> UI.getCurrent().navigate("shop/" + s.getName()));

            Button view = new Button("🔍 View", e -> UI.getCurrent().navigate("shop/" + s.getName()));

            row.add(name, view);
            shopsContainer.add(row);
        }
    }
}
