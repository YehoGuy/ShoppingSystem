package UI;

import org.springframework.beans.factory.annotation.Value;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
@CssImport("./themes/mytheme/main.css")
public class MainView extends VerticalLayout {

    public MainView(@Value("${url.api}") String api) {
        // make view fill the screen and host the blurred background
        addClassName("main");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // frosted‐glass panel
        Div content = new Div();
        content.addClassName("main-content");

        // login/register buttons
        Button login    = new Button("Login",    e -> getUI().ifPresent(u -> u.navigate("login")));
        Button register = new Button("Register", e -> getUI().ifPresent(u -> u.navigate("register")));

        // Vaadin primary + custom pill styling
        login.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        register.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        login.addClassName("pill-button");
        register.addClassName("pill-button");

        // horizontal button bar
        HorizontalLayout buttons = new HorizontalLayout(login, register);
        buttons.addClassName("hero-buttons");
        buttons.setPadding(false);
        buttons.setSpacing(true);

        // add just the button bar (no title) to the frosted card
        content.add(buttons);

        // show it
        add(content);
    }
}
