package UI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Value;

import DTOs.MemberDTO;
import DTOs.rolesDTO;

@Route(value = "profile", layout = AppLayoutBasic.class)

public class PersonProfileView extends VerticalLayout implements BeforeEnterObserver {

    @Value("${url.api}/users")
    private String USER_URL;

    @Value("${url.api}/users/notifications")
    private String NOTIF_URL;

    @Value("${url.api}/users/getAcceptedRoles")
    private String ACCEPT_ROLES_URL;

    @Value("${url.api}/shops")
    private String SHOPS_URL;

    private final RestTemplate rest = new RestTemplate();

    private final VerticalLayout detailsLayout = new VerticalLayout();
    private final VerticalLayout notificationsLayout = new VerticalLayout();
    private final VerticalLayout rolesLayout = new VerticalLayout();

    private String token;
    private int profileUserId;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // Ensure userId is set in session
        // 1) Auth
        token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            event.forwardTo("login");
            return;
        }

        handleSuspence();

        // 2) Parse ?userId= from URL
        String userIdString = VaadinSession.getCurrent().getAttribute("userId").toString();
        if (userIdString == null) {
            event.forwardTo("home");
            return;
        }
        profileUserId = Integer.parseInt(userIdString);

        // 3) Build UI
        buildLayout();
        loadProfile();
        loadNotifications();
        loadRoles();
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    private void buildLayout() {
        removeAll();

        HorizontalLayout page = new HorizontalLayout();
        page.setSizeFull();

        // ── Left: Profile details
        VerticalLayout left = new VerticalLayout(new H2("👤 Profile"));
        left.setWidth("30%");
        left.add(detailsLayout);

        // ── Middle: Notifications
        VerticalLayout mid = new VerticalLayout(new H2("🔔 Notifications"));
        mid.setWidth("30%");
        mid.add(notificationsLayout);

        // ── Right: Roles
        VerticalLayout right = new VerticalLayout(new H2("⚙️ Roles"));
        right.setWidth("30%");
        right.add(rolesLayout);

        page.add(left, mid, right);
        page.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "flex-start");

        add(page);
    }

    private void loadProfile() {
        detailsLayout.removeAll();
        try {
            ResponseEntity<MemberDTO> resp = rest.getForEntity(
                    USER_URL + "/" + profileUserId + "?token=" + token,
                    MemberDTO.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                MemberDTO memberDTO = resp.getBody();
                detailsLayout.add(
                        new Span("User ID: " + memberDTO.getMemberId()),
                        new Span("Username: " + memberDTO.getUsername()),
                        new Span("Email: " + memberDTO.getEmail()),
                        new Span("Phone: " + memberDTO.getPhoneNumber()));
            } else {
                detailsLayout.add(new Span("Cannot load profile"));
            }
        } catch (Exception ex) {
            detailsLayout.add(new Span("Error"));
        }
    }

    private void loadNotifications() {
        notificationsLayout.removeAll();
        try {
            ResponseEntity<String[]> resp = rest.getForEntity(
                    NOTIF_URL + "?authToken=" + token,
                    String[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                for (String note : resp.getBody()) {
                    notificationsLayout.add(new Span("• " + note));
                }
            } else {
                notificationsLayout.add(new Span("No notifications"));
            }
        } catch (Exception ex) {
            notificationsLayout.add(new Span("Error"));
        }
    }

    private void loadRoles() {
        rolesLayout.removeAll();
        try {
            ResponseEntity<rolesDTO[]> resp = rest.getForEntity(
                    ACCEPT_ROLES_URL + "?authToken=" + token,
                    rolesDTO[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                for (rolesDTO r : resp.getBody()) {
                    if (r.getUserName().equalsIgnoreCase( /* your MemberDTO.getUsername() */ "")) {
                        HorizontalLayout row = new HorizontalLayout();
                        DTOs.ShopDTO shop = rest.getForObject(
                                SHOPS_URL + "/" + r.getShopId() + "?authToken=" + token,
                                DTOs.ShopDTO.class);

                        String shopName = shop.getName();
                        row.add(
                                new Span(r.getRoleName() + " @ " + shopName),
                                new Span("Perms: " + String.join(",", r.getPermissions())));
                        rolesLayout.add(row);
                    }
                }
                if (rolesLayout.getComponentCount() == 0) {
                    rolesLayout.add(new Span("No roles for this user"));
                }
            } else {
                rolesLayout.add(new Span("Cannot load roles"));
            }
        } catch (Exception ex) {
            rolesLayout.add(new Span("Error"));
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
        String url = "http://localhost:8080/api/users" + "/" + userId + "/isSuspended?token=" + token;
        ResponseEntity<Boolean> response = rest.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }
}
