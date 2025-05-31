package UI;

import DTOs.MemberDTO;
import DTOs.ShopDTO;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Route(value = "admin", layout = AppLayoutBasic.class)
@JsModule("./js/notification-client.js")
public class AdminView extends VerticalLayout implements BeforeEnterObserver {

    private Grid<UserGridRow> userGrid;
    private Grid<ShopGridRow> shopGrid;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL = "http://localhost:8080";

    public static class UserGridRow {
        private int id;
        private String username;
        private String email;

        public UserGridRow() {
        }

        public UserGridRow(int id, String username, String email) {
            this.id = id;
            this.username = username;
            this.email = email;
        }

        public int getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }
    }

    public static class ShopGridRow {
        private int id;
        private String name;

        public ShopGridRow() {
        }

        public ShopGridRow(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }
        UI.getCurrent().getPage().executeJs("import(./js/notification-client.js).then(m => m.connectNotifications($0))",
                getUserId());
    }

    private String getUserId() {
        return VaadinSession.getCurrent().getAttribute("userId").toString();
    }
    
    

    public AdminView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        add(new H1("Welcome to the Admin Panel!"));

        // USERS
        add(new H2("System Users"));
        userGrid = new Grid<>(UserGridRow.class);
        userGrid.setColumns("username", "email");

        userGrid.addComponentColumn(user -> {
            Button adminBtn = new Button("Make Admin");
            Button suspendBtn = new Button("Suspend 30 Days");

            adminBtn.addClickListener(e -> promoteUserToAdmin(user.getId()));
            suspendBtn.addClickListener(e -> suspendUser(user.getId()));

            return new HorizontalLayout(adminBtn, suspendBtn);
        });

        userGrid.setWidthFull();
        add(userGrid);

        // SHOPS
        add(new H2("System Shops"));
        shopGrid = new Grid<>(ShopGridRow.class);
        shopGrid.setColumns("name");

        shopGrid.addComponentColumn(shop -> {
            Button removeBtn = new Button("Remove");
            Button viewBtn = new Button("View");

            removeBtn.addClickListener(e -> removeShop(shop.getId()));
            viewBtn.addClickListener(e -> UI.getCurrent().navigate("shop/" + shop.getId()));

            return new HorizontalLayout(removeBtn, viewBtn);
        });

        shopGrid.setWidthFull();
        add(shopGrid);

        loadUsers();
        loadShops();
    }

    private void loadUsers() {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/api/users/allmembers" + "?token=" + token;

            ResponseEntity<MemberDTO[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, MemberDTO[].class);

            List<MemberDTO> members = response.getBody() != null ? Arrays.asList(response.getBody())
                    : Collections.emptyList();

            List<UserGridRow> rows = members.stream()
                    .map(m -> new UserGridRow(m.getMemberId(), m.getUsername(), m.getEmail()))
                    .collect(Collectors.toList());

            userGrid.setItems(rows);
        } catch (Exception e) {
            Notification.show("Failed to load users: " + e.getMessage());
        }
    }

    private void loadShops() {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/api/shops/all" + "?token=" + token;

            ResponseEntity<ShopDTO[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, ShopDTO[].class);

            List<ShopDTO> shops = response.getBody() != null ? Arrays.asList(response.getBody())
                    : Collections.emptyList();

            List<ShopGridRow> rows = shops.stream()
                    .map(s -> new ShopGridRow(s.getShopId(), s.getName()))
                    .collect(Collectors.toList());

            shopGrid.setItems(rows);
        } catch (Exception e) {
            Notification.show("Failed to load shops: " + e.getMessage());
        }
    }

    private void promoteUserToAdmin(int userId) {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/api/users/" + userId + "/admin" + "?token=" + token;

            restTemplate.postForEntity(url, request, Void.class);
            Notification.show("User " + userId + " promoted to admin");
        } catch (Exception e) {
            Notification.show("Promotion failed: " + e.getMessage());
        }
    }

    private void suspendUser(int userId) {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/api/users/" + userId + "/suspension"
                    + "?token=" + token + "&until=" + LocalDateTime.now().plusMinutes(2);

            restTemplate.postForEntity(url, request, Void.class);
            Notification.show("User " + userId + " suspended");
        } catch (Exception e) {
            Notification.show("Suspension failed: " + e.getMessage());
        }
    }

    private void removeShop(int shopId) {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/api/shops/" + shopId + "?token=" + token;

            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            Notification.show("Shop " + shopId + " removed");
        } catch (Exception e) {
            Notification.show("Failed to remove shop: " + e.getMessage());
        }
    }

    private HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    private String getToken() {
        return (String) VaadinSession.getCurrent().getAttribute("authToken");
    }
}
