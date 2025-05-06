package UI;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("login")
public class LoginView extends VerticalLayout {

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        LoginForm loginForm = new LoginForm();
        loginForm.addLoginListener(event -> {
            // Handle login logic here
        });
        Button loginAsGuestButton = new Button("Login as Guest", e -> {
            // Handle guest login logic here
        });
        Button back = new Button("Back", e -> getUI().ifPresent(ui -> ui.navigate("")));

        add(loginForm, loginAsGuestButton, back);
    }
}
