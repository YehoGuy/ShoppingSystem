package UI;

import org.springframework.beans.factory.annotation.Value;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
@CssImport("./themes/mytheme/main.css")
public class MainView extends VerticalLayout {

    public MainView(@Value("${url.api}") String api) {
        // Make this view fill the screen
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("main");            // container for background + blur

        // The frosted‐glass card
        Div content = new Div();
        content.addClassName("main-content");

        // Login / Register
        Button login    = new Button("Login",    e -> getUI().ifPresent(u -> u.navigate("login")));
        Button register = new Button("Register", e -> getUI().ifPresent(u -> u.navigate("register")));

        login.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        register.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        // Wrap in a HorizontalLayout
        HorizontalLayout buttons = new HorizontalLayout(login, register);
        buttons.setSpacing(true);
        buttons.setPadding(true);
        buttons.setAlignItems(Alignment.CENTER);
        
        content.add(login, register);
        add(content);
    }
}
