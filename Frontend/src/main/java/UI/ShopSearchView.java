package UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import DTOs.ShopDTO;

@Route(value = "shops", layout = AppLayoutBasic.class)

public class ShopSearchView extends VerticalLayout implements BeforeEnterObserver {

    private final String api;
    private final String shopsApiUrl;

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

    public ShopSearchView(@Value("${url.api}") String api) {
        this.api          = api;
        this.shopsApiUrl  = api + "/shops/all";

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
            shopsContainer.add(shopLayout);
        });
        shopsContainer.setAlignItems(Alignment.CENTER);
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
        String url = api + "/users/" + userId + "/isSuspended?token=" + token;
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }
}
