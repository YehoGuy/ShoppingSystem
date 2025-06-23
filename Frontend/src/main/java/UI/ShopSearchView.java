package UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import DTOs.ShopDTO;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Route(value = "shops", layout = AppLayoutBasic.class)
public class ShopSearchView extends BaseView implements BeforeEnterObserver {

    private final String api;
    private final String shopsApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final List<ShopDTO> allShops     = new ArrayList<>();
    private final List<ShopDTO> filteredShops = new ArrayList<>();

    private final VerticalLayout shopsContainer = new VerticalLayout();

    public ShopSearchView(@Value("${url.api}") String api) {
        super("Shops", "Browse all shops", "🔍", "🏬");
        this.api         = api;
        this.shopsApiUrl = api + "/shops/all";

        /* -----------  layout basics  ----------- */
        setSizeFull();         
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);

        /* -----------  card wrapper  ----------- */
        Div card = new Div();
        card.addClassName("view-card");
        card.setWidthFull();                        
        card.getStyle()
            .set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)")
            .set("border-radius", "1rem")
            .set("padding", "2rem")
            .set("margin", "0 3vw")                
            .set("min-height", "70vh");           

        /* -----------  title  ----------- */
        H1 title = new H1("Available Shops");
        title.getStyle()
             .set("margin-top", "0")
             .set("margin-bottom", "1rem");
        card.add(title);

        /* -----------  search field  ----------- */
        TextField searchField = new TextField();
        searchField.setPlaceholder("🔍 Search shops…");
        searchField.setWidthFull();                 // fills the card’s width

        searchField.getStyle()
                   .set("background", "#ffffff")
                   .set("border", "1px solid #e2e8f0")
                   .set("border-radius", "0.5rem")
                   .set("padding", "0.75rem 1rem")
                   .set("box-shadow", "0 4px 15px rgba(0,0,0,0.1)");
        searchField.addValueChangeListener(e -> filterShops(e.getValue()));
        card.add(searchField);

        /* -----------  container for shop cards  ----------- */
        shopsContainer.setWidth("600px");          
        shopsContainer.getStyle()
                      .set("max-height", "60vh")
                      .set("padding", "1rem 0");
        card.add(shopsContainer);

        add(card);
    }

    /* --------------------  navigation guard  -------------------- */

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Integer userId = getUserId();
        String token   = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (userId == null || token == null) {
            event.forwardTo("login");
            return;
        }
        loadShops(token);
        displayShops(filteredShops);
        handleSuspence();
    }

    /* --------------------  helpers  -------------------- */

    private Integer getUserId() {
        Object uid = VaadinSession.getCurrent().getAttribute("userId");
        if (uid != null) {
            return Integer.parseInt(uid.toString());
        }
        UI.getCurrent().navigate("login");
        return null;
    }

    private void loadShops(String token) {
        try {
            String url = shopsApiUrl + "?token=" + token;
            ResponseEntity<ShopDTO[]> resp = restTemplate.getForEntity(url, ShopDTO[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                allShops.clear();
                allShops.addAll(Arrays.asList(resp.getBody()));
                filteredShops.clear();
                filteredShops.addAll(allShops);
            } else {
                Notification.show("Failed to load shops", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("Error loading shops", 5000, Notification.Position.MIDDLE);
        }
    }

    private void filterShops(String query) {
        filteredShops.clear();
        filteredShops.addAll(allShops.stream()
                                     .filter(s -> s.getName()
                                                   .toLowerCase()
                                                   .contains(query.toLowerCase()))
                                     .collect(Collectors.toList()));
        displayShops(filteredShops);
    }

    private void displayShops(List<ShopDTO> shops) {
        shopsContainer.removeAll();

        if (shops.isEmpty()) {
            shopsContainer.add(new Paragraph("No shops found."));
            return;
        }

        for (ShopDTO shop : shops) {
            Div shopCard = new Div();
            shopCard.setWidthFull();                
            shopCard.getStyle()
                    .set("background", "#ffffff")
                    .set("border-radius", "0.5rem")
                    .set("padding", "1rem")
                    .set("margin-bottom", "1rem")
                    .set("box-shadow", "0 4px 15px rgba(0,0,0,0.1)")
                    .set("cursor", "pointer");

            H1 shopName = new H1(shop.getName());
            shopName.getStyle().set("margin", "0");
            shopName.addClickListener(e ->
                    UI.getCurrent().navigate("shop/" + shop.getShopId()));

            shopCard.add(shopName);
            shopsContainer.add(shopCard);
        }
        shopsContainer.setAlignItems(Alignment.CENTER);
    }

    private void handleSuspence() {
        Integer userId = getUserId();
        String token   = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (userId == null || token == null) {
            return;
        }
        String url = api + "/users/" + userId + "/isSuspended?token=" + token;
        ResponseEntity<Boolean> resp = restTemplate.getForEntity(url, Boolean.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", resp.getBody());
        } else {
            Notification.show("Failed to check suspension status",
                              3000, Notification.Position.MIDDLE);
        }
    }
}
