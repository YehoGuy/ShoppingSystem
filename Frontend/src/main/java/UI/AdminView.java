package UI;

import DTOs.ItemDTO;
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.NumberField;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;


@Route(value = "admin", layout = AppLayoutBasic.class)

@JsModule("@vaadin/dialog/vaadin-dialog.js")
// ⬇️ Add this line so NumberField appears in the client
@JsModule("@vaadin/number-field/vaadin-number-field.js")
public class AdminView extends VerticalLayout implements BeforeEnterObserver {

    private Grid<UserGridRow> userGrid;
    private Grid<ShopGridRow> shopGrid;
    private Grid<ItemGridRow> itemGrid;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL;

    public static class UserGridRow {
        private int id;
        private String username;
        private String email;
        private String suspensionUntil;

        public UserGridRow() {
        }

        public UserGridRow(int id, String username, String email, String suspensionUntil) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.suspensionUntil = suspensionUntil;
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

        public String getSuspensionUntil() {
            return suspensionUntil;
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

    public static class ItemGridRow {
        private int id;
        private String name;
        private String description;
        private String category;

        public ItemGridRow() {
        }

        public ItemGridRow(int id, String name, String description, String category) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getCategory() {
            return category;
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // Ensure userId is set in session
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    public AdminView(@Value("${url.api}") String baseUrl) {
        this.BASE_URL = baseUrl;
        // removeAll();
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        add(new H1("Welcome to the Admin Panel!"));

        // USERS
        add(new H2("System Users"));
        userGrid = new Grid<>(UserGridRow.class);
        userGrid.setColumns("id","username", "email", "suspensionUntil");

        userGrid.addComponentColumn(user -> {
            Button adminBtn = new Button("Make Admin");
            Button suspendBtn = new Button("Suspend", e -> setSuspentionTime(user.getId()));
            Button banButton = new Button("Ban");
            Button unSuspendBtn = new Button("UnSuspend\\Unban");

            adminBtn.addClickListener(e -> promoteUserToAdmin(user.getId()));
            banButton.addClickListener(e -> banUser(user.getId()));
            unSuspendBtn.addClickListener(e -> unSuspendUser(user.getId()));
            // suspendBtn.addClickListener(e -> setSuspentionTime(user.getId()));

            HorizontalLayout buttonLayout = new HorizontalLayout(adminBtn, suspendBtn, banButton, unSuspendBtn);
            buttonLayout.setSpacing(true);
            buttonLayout.getStyle().set("flex-wrap", "wrap"); // key for responsiveness
            buttonLayout.setWidthFull(); // allows wrapping within column

            return buttonLayout;
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

        // ITEMS
        add(new H2("System Items"));
        itemGrid = new Grid<>(ItemGridRow.class);
        itemGrid.setColumns("name", "description", "category");

        itemGrid.addComponentColumn(item -> {
            // Button removeBtn = new Button("Remove");
            // Button viewBtn = new Button("View");

            // removeBtn.addClickListener(e -> removeItem(item.getId()));
            // viewBtn.addClickListener(e -> UI.getCurrent().navigate("item/" +
            // item.getId()));

            return new HorizontalLayout();// removeBtn);//, viewBtn);
        });

        itemGrid.setWidthFull();
        add(itemGrid);

        loadUsers();
        loadShops();
        loadItems();
        Button testDialogBtn = new Button("Test Dialog", e -> {
            Dialog dialog = new Dialog();
            dialog.add(new VerticalLayout(new H2("Test Dialog"), new Button("Close", ev -> dialog.close())));
            add(dialog);
            dialog.open();
        });
        add(testDialogBtn);
    }

    private String formatLocalDateTime(LocalDateTime dt) {
        if (dt == null) {
            return "";
        }
        if (dt.isBefore(LocalDateTime.now())) {
            return "";
        }
        if (dt.isEqual(LocalDateTime.of(9999, 12, 31, 23, 59))) {
            return "banned";
        }
        // Notification.show(dt.toString());
        // Example: "Jun 2, 2025 5:18:15 PM"
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault());
        return dt.format(formatter);
    }

    private void loadUsers() {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/users/allmembers?token=" + token;

            ResponseEntity<MemberDTO[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, MemberDTO[].class);

            List<MemberDTO> members = response.getBody() != null ? Arrays.asList(response.getBody())
                    : Collections.emptyList();

            List<UserGridRow> rows = members.stream()
                    .map(m -> {
                        // Format the suspensionUntil for display
                        String formatted = formatLocalDateTime(m.getSuspendedUntil());
                        return new UserGridRow(
                                m.getMemberId(),
                                m.getUsername(),
                                m.getEmail(),
                                formatted);
                    })
                    .collect(Collectors.toList());

            userGrid.setItems(rows);
        } catch (Exception e) {
            Notification.show("Failed to load users");
        }
    }

    private void loadShops() {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/shops/all?token=" + token;

            ResponseEntity<ShopDTO[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, ShopDTO[].class);

            List<ShopDTO> shops = response.getBody() != null ? Arrays.asList(response.getBody())
                    : Collections.emptyList();

            List<ShopGridRow> rows = shops.stream()
                    .map(s -> new ShopGridRow(s.getShopId(), s.getName()))
                    .collect(Collectors.toList());

            shopGrid.setItems(rows);
        } catch (Exception e) {
            Notification.show("Failed to load shops");
        }
    }

    private void loadItems() {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/items/all?token=" + token;

            ResponseEntity<ItemDTO[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, ItemDTO[].class);

            List<ItemDTO> items = response.getBody() != null ? Arrays.asList(response.getBody())
                    : Collections.emptyList();

            List<ItemGridRow> rows = items.stream()
                    .map(it -> new ItemGridRow(it.getId(), it.getName(), it.getDescription(), it.getCategory()))
                    .collect(Collectors.toList());

            itemGrid.setItems(rows);
        } catch (Exception e) {
            Notification.show("Failed to load items");
        }
    }

    private void promoteUserToAdmin(int userId) {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/users/" + userId + "/admin?token=" + token;

            restTemplate.postForEntity(url, request, Void.class);
            Notification.show("User " + userId + " promoted to admin");
        } catch (Exception e) {
            Notification.show("Promotion failed");
        }
    }

    private void suspendUser(int userId, LocalDateTime time, Dialog dialog) {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = BASE_URL + "/users/" + userId + "/suspension"
                + "?token=" + token + "&until=" + time;

            restTemplate.postForEntity(url, request, Void.class);
            loadUsers();
            Notification.show("User " + userId + " suspended until " + time);
            dialog.close();

        } catch (Exception e) {
            Notification.show("Suspension failed");
        }
    }

    private void unSuspendUser(int userId) {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/users/" + userId + "/unsuspension?token=" + token;

            restTemplate.postForEntity(url, request, Void.class);
            Notification.show("User " + userId + " unsuspended");
            loadUsers(); // Refresh the user grid
        } catch (Exception e) {
            Notification.show("Unsuspension failed");
        }
    }

    private void banUser(int userId) {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/users/" + userId + "/ban?token=" + token;

            restTemplate.postForEntity(url, request, Void.class);
            Notification.show("User " + userId + " banned");
            loadUsers(); // Refresh the user grid
        } catch (Exception e) {
            Notification.show("Ban failed");
        }
    }

    private void setSuspentionTime(int userId) {
        // 1) Create a brand-new Dialog
        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        // 2) Create all seven NumberFields
        NumberField secondsField = new NumberField("Seconds");
        NumberField minutesField = new NumberField("Minutes");
        NumberField hoursField = new NumberField("Hours");
        NumberField daysField = new NumberField("Days");
        NumberField weeksField = new NumberField("Weeks");
        NumberField monthsField = new NumberField("Months");
        NumberField yearsField = new NumberField("Years");

        // Optional: if your Vaadin version supports setMin():
        secondsField.setMin(0);
        minutesField.setMin(0);
        hoursField.setMin(0);
        daysField.setMin(0);
        weeksField.setMin(0);
        monthsField.setMin(0);
        yearsField.setMin(0);

        // 3) “Back” button just closes
        Button backBtn = new Button("Back", evt -> dialog.close());

        // 4) “Confirm” button reads each NumberField (defaults to 0), builds
        // LocalDateTime, calls suspendUser(...)
        Button confirmBtn = new Button("Confirm", evt -> {
            int secs = (secondsField.getValue() != null) ? secondsField.getValue().intValue() : 0;
            int mins = (minutesField.getValue() != null) ? minutesField.getValue().intValue() : 0;
            int hrs = (hoursField.getValue() != null) ? hoursField.getValue().intValue() : 0;
            int dys = (daysField.getValue() != null) ? daysField.getValue().intValue() : 0;
            int wks = (weeksField.getValue() != null) ? weeksField.getValue().intValue() : 0;
            int mths = (monthsField.getValue() != null) ? monthsField.getValue().intValue() : 0;
            int yrs = (yearsField.getValue() != null) ? yearsField.getValue().intValue() : 0;

            LocalDateTime suspensionTime = makeTime(secs, mins, hrs, dys, wks, mths, yrs);
            suspendUser(userId, suspensionTime, dialog);
        });

        // 5) Arrange fields + buttons in a VerticalLayout
        VerticalLayout content = new VerticalLayout(
                secondsField,
                minutesField,
                hoursField,
                daysField,
                weeksField,
                monthsField,
                yearsField,
                new HorizontalLayout(backBtn, confirmBtn));
        content.setPadding(true);
        content.setSpacing(true);

        dialog.add(content);

        // 6) Attach to UI root and open
        UI.getCurrent().add(dialog);
        dialog.open();
    }

    private void removeShop(int shopId) {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/shops/" + shopId + "?token=" + token;

            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            Notification.show("Shop " + shopId + " removed");
            loadShops();
        } catch (Exception e) {
            Notification.show("Failed to remove shop");
        }
    }

    private void removeItem(int itemId) {
        try {
            String token = getToken();
            HttpHeaders headers = getHeaders(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = BASE_URL + "/items/" + itemId + "?token=" + token;

            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            Notification.show("Item " + itemId + " removed");
        } catch (Exception e) {
            Notification.show("Failed to remove item");
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

    private LocalDateTime makeTime(int seconds, int minutes, int hours, int days, int weeks, int months, int years) {
        LocalDateTime time = LocalDateTime.now();
        if (seconds != 0)
            time = time.plusSeconds(seconds);
        if (minutes != 0)
            time = time.plusMinutes(minutes);
        if (hours != 0)
            time = time.plusHours(hours);
        if (days != 0)
            time = time.plusDays(days);
        if (weeks != 0)
            time = time.plusWeeks(weeks);
        if (months != 0)
            time = time.plusMonths(months);
        if (years != 0)
            time = time.plusYears(years);
        return time;
    }
}
