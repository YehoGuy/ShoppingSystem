package UI;

import javax.swing.GroupLayout.Alignment;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

@Route(value = "home", layout = AppLayoutBasic.class)

public class HomeView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    public HomeView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        H1 title = new H1("Welcome to the Home Page!");
        add(title);

    }
}
