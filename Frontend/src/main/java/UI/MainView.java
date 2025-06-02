package UI;

import java.io.ObjectInputFilter.Config;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import Config.UrlService;

@Route("")
public class MainView extends VerticalLayout {

    public MainView() {


        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName(LumoUtility.Background.CONTRAST_5); // Light background

        H1 title = new H1("Welcome to Shopping System");
        title.addClassName(LumoUtility.Margin.Bottom.LARGE);

        Button loginButton = new Button("Login", e -> login());
        Button registerButton = new Button("Register", e -> register());

        loginButton.addClassNames(
                LumoUtility.Margin.Bottom.MEDIUM,
                LumoUtility.Padding.SMALL,
                LumoUtility.FontSize.LARGE);

        registerButton.addClassNames(
                LumoUtility.Padding.SMALL,
                LumoUtility.FontSize.LARGE);

        add(title, loginButton, registerButton);
    }

    private void login() {
        String url = UrlService.getApiUrl();
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.add("API URL: " + url);
        dialog.setCloseOnOutsideClick(true);
        dialog.open();
        //getUI().ifPresent(ui -> ui.navigate("login"));
    }

    private void register() {
        getUI().ifPresent(ui -> ui.navigate("register"));
    }
}
