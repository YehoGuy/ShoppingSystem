package UI;

import DTOs.MemberDTO;
import DTOs.ShopDTO;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Route(value = "admin", layout = AppLayoutBasic.class)
public class AdminView extends VerticalLayout implements BeforeEnterObserver {

    private Grid<UserGridRow> userGrid;
    private Grid<ShopGridRow> shopGrid;
    private final ObjectMapper mapper = new ObjectMapper();

    public static class UserGridRow {
        private int id;
        private String username;
        private String email;

        public UserGridRow() {}
        public UserGridRow(int id, String username, String email) {
            this.id = id;
            this.username = username;
            this.email = email;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
    }

    public static class ShopGridRow {
        private int id;
        private String name;

        public ShopGridRow() {}
        public ShopGridRow(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }
        public String getName() { return name; }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }
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
            String endpoint = "/api/users/allmembers?token=" + token;
            ResponseEntity<MemberDTO[]> response = sendGetRequest(endpoint, new TypeReference<>() {});
            List<MemberDTO> members = response.getBody() != null ? Arrays.asList(response.getBody()) : Collections.emptyList();
            if (members.isEmpty()) {
                Notification.show("No users found");
                return;
            }
            List<UserGridRow> rows = members.stream()
                    .map(m -> new UserGridRow(m.memberId(), m.username(), m.email()))
                    .collect(Collectors.toList());
            userGrid.setItems(rows);
        } catch (Exception e) {
            Notification.show("Failed to load users: " + e.getMessage());
        }
    }

    private void loadShops() {
        try {
            String token = getToken();
            String endpoint = "/api/shops/all?token=" + token;
            ResponseEntity<ShopDTO[]> response = sendGetRequest(endpoint, new TypeReference<>() {});

            List<ShopDTO> shops = response.getBody() != null ? Arrays.asList(response.getBody()) : Collections.emptyList();
                if (shops.isEmpty()) {
                        Notification.show("No shops found");
                        return;
                }
            List<ShopGridRow> rows = shops.stream()
                    .map(s -> new ShopGridRow(s.getShopId(), s.getName()))
                    .collect(Collectors.toList());
            shopGrid.setItems(rows);
        } catch (Exception e) {
            Notification.show("Failed to load shops: " + e.getMessage());
        }
    }

    private void promoteUserToAdmin(int userId) {
        String token = getToken();
        String url = "/api/users/" + userId + "/admin?token=" + token;
        sendPostRequest(url);
        Notification.show("Promoted user " + userId + " to admin");
    }

    private void suspendUser(int userId) {
        String token = getToken();
        String url = "/api/users/" + userId + "/suspension?token=" + token + "&until=" + LocalDateTime.now().plusDays(30);
        sendPatchRequest(url);
        Notification.show("Suspended user " + userId + " for 30 days");
    }

    private void removeShop(int shopId) {
        String token = getToken();
        String url = "/api/shops/" + shopId + "?token=" + token;
        sendDeleteRequest(url);
        Notification.show("Removed shop " + shopId);
    }

    // Token helper
    private String getToken() {
        return (String) VaadinSession.getCurrent().getAttribute("authToken");
    }

    // Network helpers
    private <T> T sendGetRequest(String path, TypeReference<T> typeRef) throws Exception {
        URL url = new URL("http://localhost:8080" + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        return mapper.readValue(conn.getInputStream(), typeRef);
    }

    private void sendPostRequest(String path) {
        try {
            URL url = new URL("http://localhost:8080" + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.getResponseCode();
        } catch (Exception e) {
            Notification.show("POST failed: " + e.getMessage());
        }
    }

    private void sendPatchRequest(String path) {
        try {
            URL url = new URL("http://localhost:8080" + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PATCH");
            conn.setDoOutput(true);
            conn.getResponseCode();
        } catch (Exception e) {
            Notification.show("PATCH failed: " + e.getMessage());
        }
    }

    private void sendDeleteRequest(String path) {
        try {
            URL url = new URL("http://localhost:8080" + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.getResponseCode();
        } catch (Exception e) {
            Notification.show("DELETE failed: " + e.getMessage());
        }
    }
}
