// File: src/main/java/UI/HomeView.java
package UI;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route(value = "home", layout = AppLayoutBasic.class)
@CssImport("./themes/mytheme/home.css")
public class HomeView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    @ClientCallable
    public void showNotificationFromJS(String message) {
        Notification.show(message, 5000, Notification.Position.TOP_CENTER);
    }

    public HomeView() {
        addClassName("home");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Div content = new Div();
        content.addClassName("home-content");

        H1 title = new H1("Welcome to the Home Page!");
        title.getStyle().set("font-size", "4rem");
        content.add(title);

        add(content);
    }
}