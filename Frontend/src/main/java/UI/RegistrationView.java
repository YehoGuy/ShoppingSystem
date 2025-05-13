package UI;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Route("register")
public class RegistrationView extends VerticalLayout {

    private static final String REGISTER_API_URL = "http://localhost:8080/api/users/register";
    private final RestTemplate restTemplate = new RestTemplate();

    public RegistrationView() {
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
                String url = REGISTER_API_URL
                        + "?username={username}&password={password}&email={email}&phoneNumber={phoneNumber}&address={address}";

                ResponseEntity<String> response = restTemplate.postForEntity(url, request, Void.class, params);

                if (response.getStatusCode() == HttpStatus.CREATED) {
                    VaadinSession.getCurrent().getSession().setAttribute("username", user);
                    VaadinSession.getCurrent().getSession().setAttribute("email", mail);
                    VaadinSession.getCurrent().getSession().setAttribute("phoneNumber", phoneNum);
                    VaadinSession.getCurrent().getSession().setAttribute("address", addr);
                    VaadinSession.getCurrent().getSession().setAttribute("authToken", response.getBody());
                    Notification.show("Registration successful!");
                    getUI().ifPresent(ui -> ui.navigate("home"));
                } else {
                    Notification.show("Registration failed (" + response.getStatusCode() + ")");
                }
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        Button back = new Button("Back", e -> getUI().ifPresent(ui -> ui.navigate("")));

        add(new H2("Register"), username, email, password, phone, address, register, back);
    }
}
