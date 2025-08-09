package UI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import DTOs.MemberDTO;
import DTOs.rolesDTO;

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

    private String currentUsername = null;

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
                currentUsername = m.getUsername(); // Store the current user's username
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
                rolesDTO[] roles = resp.getBody();
                
                for (rolesDTO r : roles) {
                    // Check if this role belongs to the current user
                    if (currentUsername != null) {
                        VerticalLayout roleCard = new VerticalLayout();
                        roleCard.setPadding(false);
                        roleCard.setSpacing(false);
                        roleCard.getStyle()
                            .set("margin-bottom", "10px")
                            .set("padding", "8px")
                            .set("border", "1px solid #e0e0e0")
                            .set("border-radius", "4px")
                            .set("background-color", "#f9f9f9");
                        
                        String shopName = "Shop " + r.getShopId(); // Default fallback
                        try {
                            // Try different parameter formats for the shop API
                            DTOs.ShopDTO shop = null;
                            try {
                                // First try with authToken parameter
                                shop = rest.getForObject(
                                    shopsUrl + "/" + r.getShopId() + "?token=" + token,
                                    DTOs.ShopDTO.class);
                            } catch (Exception e1) {
                                // add a span with text that says the error
                                Span errorSpan = new Span("Error loading shop details");
                                errorSpan.getStyle().set("color", "red");
                                roleCard.add(errorSpan);
                            }
                            
                            if (shop != null) {
                                shopName = shop.getName();
                            }
                        } catch (Exception ex) {
                            // Shop lookup failed, use fallback
                        }
                        
                        Span roleSpan = new Span(r.getRoleName() + " @ " + shopName);
                        roleSpan.getStyle().set("font-weight", "bold");
                        
                        Span permsSpan = new Span("Permissions: " + String.join(", ", r.getPermissions()));
                        permsSpan.getStyle()
                            .set("word-wrap", "break-word")
                            .set("white-space", "normal")
                            .set("font-size", "0.9em")
                            .set("color", "#666");
                        
                        roleCard.add(roleSpan, permsSpan);
                        rolesLayout.add(roleCard);
                    }
                }
                if (rolesLayout.getComponentCount() == 0) {
                    rolesLayout.add(new Span("No roles for this user"));
                }
            } else {
                rolesLayout.add(new Span("Cannot load roles - Status: " + resp.getStatusCode()));
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
