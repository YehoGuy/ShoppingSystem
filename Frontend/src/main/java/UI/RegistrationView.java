package UI;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("register")
public class RegistrationView extends VerticalLayout {

    public RegistrationView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        TextField username = new TextField("Username");
        EmailField email = new EmailField("Email");
        PasswordField password = new PasswordField("Password");

        Button register = new Button("Register", e -> {
            // Handle registration logic here
        });

        Button back = new Button("Back", e -> getUI().ifPresent(ui -> ui.navigate("")));

        add(new H2("Register"), username, email, password, register, back);
    }
}
