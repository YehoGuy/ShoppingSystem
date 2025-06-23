package UI;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Route("login")
public class LoginView extends BaseView {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;
    private final String authUrl;

    public LoginView(@Value("${url.api}") String api) {
        /* ── Header ───────────────────────────────────────────────────── */
        super("Member Login", "Access your account", "🔒", "➡️");

        this.baseUrl = api + "/users";
        this.authUrl  = api + "/auth";

        /* ── Card container ───────────────────────────────────────────── */
        Div card = new Div();
        card.addClassName("view-card");
        card.setWidth("400px");

        /* ── Login form ───────────────────────────────────────────────── */
        LoginForm loginForm = new LoginForm();
        loginForm.setI18n(createCustomLoginI18n());
        loginForm.addLoginListener(event -> {
            String username = event.getUsername();
            String password = event.getPassword();
            try {
                String token = loginAsMember(username, password);
                VaadinSession.getCurrent().setAttribute("authToken", token);
                VaadinSession.getCurrent().setAttribute("username", username);
                loginForm.setError(false);
                Notification.show("✅ Login successful!");
                setUserId();
                getUI().ifPresent(ui -> ui.navigate("home"));
            } catch (Exception ex) {
                loginForm.setError(true);
                Notification.show("❌ Login failed", 3000, Notification.Position.MIDDLE);
            }
        });

        /* ── Buttons ─────────────────────────────────────────────────── */
        Button guestButton = new Button("Login as Guest", e -> {
            try {
                String token = loginAsGuest();
                VaadinSession.getCurrent().setAttribute("authToken", token);
                VaadinSession.getCurrent().setAttribute("username", "guest");
                Notification.show("✅ Logged in as guest!");
                VaadinSession.getCurrent().setAttribute("isAdmin", false);
                getUI().ifPresent(ui -> ui.navigate("home"));
            } catch (Exception ex) {
                Notification.show("❌ Guest login failed", 3000, Notification.Position.MIDDLE);
            }
        });

        Button backButton = new Button("Back", e -> getUI().ifPresent(ui -> ui.navigate("")));

        guestButton.setWidth("200px");
        backButton.setWidth("120px");

        VerticalLayout buttonsColumn = new VerticalLayout(guestButton, backButton);
        buttonsColumn.setWidthFull();
        buttonsColumn.setPadding(false);
        buttonsColumn.setSpacing(true);
        buttonsColumn.setAlignItems(FlexComponent.Alignment.CENTER);

        /* ── Assemble card ────────────────────────────────────────────── */
        card.add(loginForm, buttonsColumn);
        add(card);
        setAlignItems(Alignment.CENTER);
    }

    /* ───────────────────────── עזרי אימות ─────────────────────────── */
    private String loginAsMember(String username, String password) {
        String url = baseUrl + "/login/member";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            Notification.show("❌ Member login failed");
            return null;
        }
    }

    private String loginAsGuest() {
        String url = baseUrl + "/login/guest";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(new LinkedMultiValueMap<>(), headers);
        ResponseEntity<String> response =
                restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            Notification.show("❌ Guest login failed");
            return null;
        }
    }

    private LoginI18n createCustomLoginI18n() {
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("Member Login");
        i18n.getForm().setUsername("Username");
        i18n.getForm().setPassword("Password");
        i18n.getForm().setSubmit("Login");
        i18n.getErrorMessage().setTitle("Login Failed");
        i18n.getErrorMessage().setMessage("Invalid credentials. Please try again.");
        return i18n;
    }

    private void setUserId() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) return;

        String url = authUrl + "/validate?authToken=" + token;
        ResponseEntity<Integer> response = restTemplate.getForEntity(url, Integer.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("userId", response.getBody());
            handleSuspence();
            handleAdmin();
        } else {
            Notification.show("❌ Failed to retrieve user ID");
        }
    }

    private void handleAdmin() {
        Integer userId = (Integer) VaadinSession.getCurrent().getAttribute("userId");
        if (userId == null) return;
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) return;

        String url = baseUrl + "/" + userId + "/isAdmin?token=" + token;
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isAdmin", response.getBody());
        } else {
            Notification.show("❌ Failed to check admin status");
        }
    }

    private void handleSuspence() {
        Integer userId = (Integer) VaadinSession.getCurrent().getAttribute("userId");
        if (userId == null) return;
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) return;

        String url = baseUrl + "/" + userId + "/isSuspended?token=" + token;
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show("❌ Failed to check suspension status", 3000,
                              Notification.Position.MIDDLE);
        }
    }
}
