package UI;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.beans.factory.annotation.Value;

@Route("logout")
public class LogoutView extends VerticalLayout implements BeforeEnterObserver {

    @Value("${url.api}/users/logout")
    private String LOGOUT_API_URL;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Check if the user is logged in
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
        UI.getCurrent().getPage().executeJs("window.connectWebSocket($0);", getUserId());
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") == null) {
            Notification.show("You are not connected.");
            UI.getCurrent().navigate("");
        }
        return (Integer) VaadinSession.getCurrent().getAttribute("userId");
    }

    public LogoutView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        add(new H2("Logout"));
        Button logoutButton = new Button("Logout", e -> performLogout());
        add(logoutButton);
    }

    private void performLogout() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            Notification.show("No user is logged in", 3000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("");
            return;
        }

        // Append token as request parameter
        String urlWithParam = LOGOUT_API_URL + "?token=" + token;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    urlWithParam,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);

            // Treat any 2xx response as success
            if (response.getStatusCode().is2xxSuccessful()) {
                // Clear the authToken attribute
                VaadinSession.getCurrent().setAttribute("authToken", null);
                Notification.show("Logged out successfully", 3000, Notification.Position.MIDDLE);
            } else {
                Notification.show("Logout failed: " + response.getStatusCode(), 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            Notification.show("Error during logout: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }

        UI.getCurrent().navigate("");
    }
}
