package UI;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.vaadin.flow.server.VaadinSession;


import org.springframework.beans.factory.annotation.Value;

@Route("register")
public class RegistrationView extends BaseView {
    private final String api, registerUrl, authUrl, baseUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public RegistrationView(@Value("${url.api}") String api) {
        super("Create Account", "Join our community", "✨", "📝");
        this.api = api;
        this.registerUrl = api + "/users/register";
        this.authUrl = api + "/auth";
        this.baseUrl = api + "/users";

        VerticalLayout card = new VerticalLayout();
        card.addClassName("view-card");
        card.getStyle()
            .set("background", "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)")
            .set("padding", "2rem")
            .set("border-radius", "1rem");
        card.setWidth("400px");
        card.setAlignItems(Alignment.CENTER);
        card.setSpacing(true);

        TextField username = new TextField("Username");
        username.setWidth("100%");
        username.getStyle()
            .set("background", "#eef5ff")
            .set("border", "none")
            .set("border-radius", "0.5rem")
            .set("padding", "0.75rem 1rem");

        EmailField email = new EmailField("Email");
        email.setWidth("100%");
        email.getStyle()
            .set("background", "#eef5ff")
            .set("border", "none")
            .set("border-radius", "0.5rem")
            .set("padding", "0.75rem 1rem");

        PasswordField password = new PasswordField("Password");
        password.setRevealButtonVisible(true);
        password.setWidth("100%");
        password.getStyle()
            .set("background", "#eef5ff")
            .set("border", "none")
            .set("border-radius", "0.5rem")
            .set("padding", "0.75rem 1rem");

        TextField phone = new TextField("Phone Number");
        phone.setWidth("100%");
        phone.getStyle()
            .set("background", "#eef5ff")
            .set("border", "none")
            .set("border-radius", "0.5rem")
            .set("padding", "0.75rem 1rem");

        TextField address = new TextField("Address");
        address.setWidth("100%");
        address.getStyle()
            .set("background", "#eef5ff")
            .set("border", "none")
            .set("border-radius", "0.5rem")
            .set("padding", "0.75rem 1rem");

        Button register = new Button("Register", e -> handleRegister(
            username.getValue(), password.getValue(), email.getValue(), phone.getValue(), address.getValue()
        ));
        styleButton(register);

        Button back = new Button("Back", e -> getUI().ifPresent(ui -> ui.navigate("")));
        styleButton(back);

        card.add(username, email, password, phone, address, register, back);
        add(card);
        setAlignItems(Alignment.CENTER);
    }

    private void handleRegister(String user, String pass, String email, String phone, String addr) {
        if(user.isEmpty() || pass.isEmpty() || email.isEmpty() || phone.isEmpty() || addr.isEmpty()){
            Notification.show("Please fill in all fields",3000,Notification.Position.MIDDLE);
            return;
        }
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(
                registerUrl + "?username={u}&password={p}&email={e}&phoneNumber={ph}&address={a}",
                new HttpEntity<>(null, new HttpHeaders()), String.class, user, pass, email, phone, addr
            );
            if(resp.getStatusCode()==HttpStatus.CREATED){
                VaadinSession.getCurrent().setAttribute("authToken", resp.getBody());
                Notification.show("✅ Registration successful!");
                setUserId(); 
                getUI().ifPresent(ui->ui.navigate("home"));
            } else {
                Notification.show("❌ Registration failed");
            }
        } catch(Exception ex) {
            Notification.show("❌ Error during registration",3000,Notification.Position.MIDDLE);
        }
    }

    private void styleButton(Button btn) {
        btn.setWidth("100%"); btn.addClassName("gradient-button");
    }

    private void setUserId() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken"); if(token==null)return;
        ResponseEntity<Integer> resp = restTemplate.getForEntity(authUrl+"/validate?authToken="+token,Integer.class);
        if(resp.getStatusCode().is2xxSuccessful()) 
        {
            VaadinSession.getCurrent().setAttribute("userId",resp.getBody());
            handleAdmin();
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
}