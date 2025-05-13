package UI;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route(value = "home", layout = AppLayoutBasic.class)

public class HomeView extends VerticalLayout {
    public HomeView() {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            UI.getCurrent().navigate("login");
        }
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        H1 title = new H1("Welcome to the Home Page!");
        add(title);

    }
}
