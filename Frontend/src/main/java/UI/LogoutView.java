package UI;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@Route("logout")
public class LogoutView extends BaseView implements BeforeEnterObserver {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String logoutApiUrl;

    public LogoutView(@Value("${url.api}") String api) {
        // header: title + icons
        super("Logout", "End your session securely", "🚪", "👋");

        this.logoutApiUrl = api + "/users/logout";

        // center everything
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // card container
        VerticalLayout card = new VerticalLayout();
        card.addClassName("view-card");
        card.getStyle()
                .set("background", "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)")
                .set("padding", "2rem")
                .set("border-radius", "1rem");
        card.setWidth("360px");
        card.setAlignItems(Alignment.CENTER);
        card.setSpacing(true);

        // prompt
        H2 prompt = new H2("Are you sure you want to logout?");
        prompt.getStyle()
                .set("color", "white")
                .set("margin", "0");

        // logout button
        Button logoutBtn = new Button("Logout", e -> performLogout());
        logoutBtn.getStyle()
                .set("width", "100%")
                .set("background", "linear-gradient(135deg, #ff5f6d 0%, #ffc371 100%)")
                .set("color", "white")
                .set("border", "none")
                .set("border-radius", "0.5rem")
                .set("padding", "0.75rem 1rem");

        // Add into card
        card.add(prompt, logoutBtn);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // if not logged in, redirect
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    private void performLogout() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            Notification.show("❌ No user is logged in", 3000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("");
            return;
        }

        String url = logoutApiUrl + "?token=" + token;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(headers), String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                VaadinSession.getCurrent().setAttribute("authToken", null);
                Notification.show("✅ Logged out successfully", 3000, Notification.Position.MIDDLE);
            } else {
                Notification.show("❌ Logout failed", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("❌ Error during logout", 5000, Notification.Position.MIDDLE);
        }
        disconnectFromWebSocket();
        UI.getCurrent().navigate("");
    }

    private void disconnectFromWebSocket() {
        UI.getCurrent()
                .getPage()
                .executeJs("window.disconnectWebSocket();");
    }
}
