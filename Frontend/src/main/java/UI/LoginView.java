package UI;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Route("login")
public class LoginView extends VerticalLayout {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL = "http://localhost:8080/api/users";
    private final String AUTH_URL = "http://localhost:8080/api/auth";

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

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
                Notification.show("Login successful!");
                setUserId();
                getUI().ifPresent(ui -> ui.navigate("home"));
            } catch (Exception ex) {
                loginForm.setError(true);
                Notification.show("Login failed: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        Button guestButton = new Button("Login as Guest", e -> {
            try {
                String token = loginAsGuest();
                VaadinSession.getCurrent().setAttribute("authToken", token);
                VaadinSession.getCurrent().setAttribute("username", "guest");
                Notification.show("Logged in as guest!");
                getUI().ifPresent(ui -> ui.navigate("home"));
            } catch (Exception ex) {
                Notification.show("Guest login failed: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        Button backButton = new Button("Back", e -> getUI().ifPresent(ui -> ui.navigate("")));

        add(loginForm, guestButton, backButton);
    }

    private String loginAsMember(String username, String password) {
        String url = BASE_URL + "/login/member";
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
            throw new RuntimeException("Member login failed: HTTP " + response.getStatusCodeValue());
        }
    }

    private String loginAsGuest() {
        String url = BASE_URL + "/login/guest";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(new LinkedMultiValueMap<>(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Guest login failed: HTTP " + response.getStatusCodeValue());
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
        if (token == null) {
            return;
        }

        // build the URL with the authToken as a query‐param
        String url = AUTH_URL + "/validate?authToken=" + token;

        // simply use GET—no HttpHeaders object needed
        
        ResponseEntity<Integer> response = restTemplate.getForEntity(url, Integer.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("userId", response.getBody());
            handleAdmin();
        } else {
            throw new RuntimeException(
                "Failed to retrieve user ID: HTTP " + response.getStatusCode().value()
            );
        }
    }

    private void handleAdmin() {
        Integer userId = (Integer) VaadinSession.getCurrent().getAttribute("userId");
        if (userId == null) {
            return;
        }
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            return;
        }
        String url = BASE_URL + "/"+userId+"/isAdmin?token=" +token;
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isAdmin", response.getBody());
        } else {
            throw new RuntimeException(
                "Failed to check admin status: HTTP " + response.getStatusCode().value()
            );
        }
    }
}

