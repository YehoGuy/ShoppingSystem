package UI;

import DTOs.MemberDTO;
import DTOs.rolesDTO;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Route(value = "profile", layout = AppLayoutBasic.class)
public class PersonProfileView extends BaseView implements BeforeEnterObserver {

    private final String userUrl;
    private final String notificationsUrl;
    private final String acceptedRolesUrl;
    private final String shopsUrl;

    private final RestTemplate rest = new RestTemplate();

    private final VerticalLayout detailsLayout = new VerticalLayout();
    private final VerticalLayout notificationsLayout = new VerticalLayout();
    private final VerticalLayout rolesLayout = new VerticalLayout();

    private String token;
    private int profileUserId;

    public PersonProfileView(@Value("${url.api}") String api) {
        super("Profile", "Manage your account", "👤", "⚙️");

        this.userUrl          = api + "/users";
        this.notificationsUrl = api + "/users/notifications";
        this.acceptedRolesUrl = api + "/users/getAcceptedRoles";
        this.shopsUrl         = api + "/shops";

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // redirect if not authenticated
        token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            event.forwardTo("login");
            return;
        }

        // ensure userId
        Object uid = VaadinSession.getCurrent().getAttribute("userId");
        if (uid == null) {
            event.forwardTo("home");
            return;
        }
        profileUserId = (Integer) uid;

        handleSuspence();

        buildLayout();
        loadProfile();
        loadNotifications();
        loadRoles();
    }

    private void buildLayout() {
        removeAll(); // clear old content but keep header

        HorizontalLayout row = new HorizontalLayout();
        row.setSizeFull();
        row.setSpacing(true);
        row.setJustifyContentMode(JustifyContentMode.EVENLY);

        // Profile Details card
        VerticalLayout profileCard = new VerticalLayout(new H2("👤 Profile"));
        profileCard.add(detailsLayout);
        styleCard(profileCard);

        // Notifications card
        VerticalLayout notifCard = new VerticalLayout(new H2("🔔 Notifications"));
        notifCard.add(notificationsLayout);
        styleCard(notifCard);

        // Roles card
        VerticalLayout rolesCard = new VerticalLayout(new H2("⚙️ Roles"));
        rolesCard.add(rolesLayout);
        styleCard(rolesCard);

        row.add(profileCard, notifCard, rolesCard);
        add(row);
    }

    private void styleCard(VerticalLayout card) {
        card.addClassName("view-card");
        card.setWidth("30%");
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
            .set("background", "linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)")
            .set("border-radius", "1rem")
            .set("box-shadow", "0 8px 32px rgba(0,0,0,0.1)");
    }

    private void loadProfile() {
        detailsLayout.removeAll();
        try {
            ResponseEntity<MemberDTO> resp = rest.getForEntity(
                userUrl + "/" + profileUserId + "?token=" + token,
                MemberDTO.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                MemberDTO m = resp.getBody();
                detailsLayout.add(
                    new Span("User ID: " + m.getMemberId()),
                    new Span("Username: " + m.getUsername()),
                    new Span("Email: " + m.getEmail()),
                    new Span("Phone: " + m.getPhoneNumber())
                );
            } else {
                detailsLayout.add(new Span("Cannot load profile"));
            }
        } catch (Exception ex) {
            detailsLayout.add(new Span("Error loading profile"));
        }
    }

    private void loadNotifications() {
        notificationsLayout.removeAll();
        try {
            ResponseEntity<String[]> resp = rest.getForEntity(
                notificationsUrl + "?authToken=" + token,
                String[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                for (String note : resp.getBody()) {
                    notificationsLayout.add(new Span("• " + note));
                }
            } else {
                notificationsLayout.add(new Span("No notifications"));
            }
        } catch (Exception ex) {
            notificationsLayout.add(new Span("Error loading notifications"));
        }
    }

    private void loadRoles() {
        rolesLayout.removeAll();
        try {
            ResponseEntity<rolesDTO[]> resp = rest.getForEntity(
                acceptedRolesUrl + "?authToken=" + token,
                rolesDTO[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                for (rolesDTO r : resp.getBody()) {
                    if (r.getUserName().equalsIgnoreCase(/* current username */ "")) {
                        HorizontalLayout line = new HorizontalLayout();
                        DTOs.ShopDTO shop = rest.getForObject(
                            shopsUrl + "/" + r.getShopId() + "?authToken=" + token,
                            DTOs.ShopDTO.class);
                        line.add(
                            new Span(r.getRoleName() + " @ " + shop.getName()),
                            new Span("Perms: " + String.join(",", r.getPermissions()))
                        );
                        rolesLayout.add(line);
                    }
                }
                if (rolesLayout.getComponentCount() == 0) {
                    rolesLayout.add(new Span("No roles for this user"));
                }
            } else {
                rolesLayout.add(new Span("Cannot load roles"));
            }
        } catch (Exception ex) {
            rolesLayout.add(new Span("Error loading roles"));
        }
    }

    private void handleSuspence() {
        Integer userId = (Integer) VaadinSession.getCurrent().getAttribute("userId");
        if (userId == null) return;
        ResponseEntity<Boolean> resp = rest.getForEntity(
            userUrl + "/" + userId + "/isSuspended?token=" + token,
            Boolean.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", resp.getBody());
        } else {
            Notification.show("Failed to check suspension status");
        }
    }
}
