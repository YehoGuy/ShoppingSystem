// package UI;

// import com.vaadin.flow.component.button.Button;
// import com.vaadin.flow.component.login.LoginForm;
// import com.vaadin.flow.component.orderedlayout.VerticalLayout;
// import com.vaadin.flow.router.Route;

// @Route("login")
// public class LoginView extends VerticalLayout {

//     public LoginView() {
//         setSizeFull();
//         setAlignItems(Alignment.CENTER);
//         setJustifyContentMode(JustifyContentMode.CENTER);

//         LoginForm loginForm = new LoginForm();
//         loginForm.addLoginListener(event -> {
//             // Handle login logic here
//         });
//         Button loginAsGuestButton = new Button("Login as Guest", e -> {
//             // Handle guest login logic here
//         });
//         Button back = new Button("Back", e -> getUI().ifPresent(ui -> ui.navigate("")));

//         add(loginForm, loginAsGuestButton, back);
//     }
// } 



////////////////////////////////////////////// this was old class ///////////////////////////////////
package UI;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Route("login")
public class LoginView extends VerticalLayout {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL = "http://localhost:8080/api/users";

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
                Notification.show("Login successful! Token: " + token);
                getUI().ifPresent(ui -> ui.navigate("home"));
                // Optionally: store token in session or navigate to profile
            } catch (Exception ex) {
                loginForm.setError(true);
                Notification.show("Login failed: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        Button loginAsGuestButton = new Button("Login as Guest", e -> {
            try {
                String token = loginAsGuest();
                Notification.show("Logged in as guest! Token: " + token);
                getUI().ifPresent(ui -> ui.navigate("home"));

                // Optionally: store token in session or navigate to guest profile
            } catch (Exception ex) {
                Notification.show("Guest login failed: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        Button back = new Button("Back", e -> getUI().ifPresent(ui -> ui.navigate("")));

        add(loginForm, loginAsGuestButton, back);
    }

    private String loginAsGuest() {
        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/login/guest", null, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody(); // This is the token
        } else {
            throw new RuntimeException("Guest login failed");
        }
    }

    private String loginAsMember(String username, String password) {
        String url = BASE_URL + "/login/member?username={username}&password={password}";
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);

        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class, params);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody(); // This is the token
        } else {
            throw new RuntimeException("Member login failed");
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
}

