package UI;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.router.Route;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.vaadin.flow.server.VaadinSession;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

@Route("register")
public class RegistrationView extends VerticalLayout {

    private final String api;
    private final String registerUrl;
    private final String authUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public RegistrationView(@Value("${url.api}") String api) {
        this.api         = api;
        this.registerUrl = api + "/users/register";
        this.authUrl     = api + "/auth";

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        TextField username = new TextField("Username");
        EmailField email = new EmailField("Email");
        PasswordField password = new PasswordField("Password");
        TextField phone = new TextField("Phone Number");
        TextField address = new TextField("Address");

        Button register = new Button("Register", e -> {
            String user = username.getValue();
            String pass = password.getValue();
            String mail = email.getValue();
            String phoneNum = phone.getValue();
            String addr = address.getValue();

            if (user.isEmpty() || pass.isEmpty() || mail.isEmpty() || phoneNum.isEmpty() || addr.isEmpty()) {
                Notification.show("Please fill in all fields", 3000, Notification.Position.MIDDLE);
                return;
            }

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                Map<String, String> params = new HashMap<>();
                params.put("username", user);
                params.put("password", pass);
                params.put("email", mail);
                params.put("phoneNumber", phoneNum);
                params.put("address", addr);

                HttpEntity<?> request = new HttpEntity<>(null, headers);

                // build query string
                String url = registerUrl
                    + "?username={username}&password={password}&email={email}"
                    + "&phoneNumber={phoneNumber}&address={address}";

                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class, params);

                if (response.getStatusCode() == HttpStatus.CREATED) {
                    VaadinSession session = VaadinSession.getCurrent();

                    session.setAttribute("username", user);
                    session.setAttribute("email", mail);
                    session.setAttribute("phoneNumber", phoneNum);
                    session.setAttribute("address", addr);
                    session.setAttribute("authToken", response.getBody());

                    Notification.show("Registration successful!");
                    setUserId();
                    getUI().ifPresent(ui -> ui.navigate("home"));
                } else {
                    Notification.show("Registration failed");
                }
            } catch (Exception ex) {
                Notification.show("Error", 5000, Notification.Position.MIDDLE);
            }
        });

        Button back = new Button("Back", e -> getUI().ifPresent(ui -> ui.navigate("")));

        add(new H2("Register"), username, email, password, phone, address, register, back);
    }

    private void setUserId() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            return;
        }

        // build the URL with the authToken as a query‐param
        String url = authUrl + "/validate?authToken=" + token;

        // simply use GET—no HttpHeaders object needed
        ResponseEntity<Integer> response = restTemplate.getForEntity(url, Integer.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("userId", response.getBody());
            VaadinSession.getCurrent().setAttribute("isAdmin", false);

        } else {
            Notification.show(
                    "Failed to retrieve user ID");
        }
    }
}
