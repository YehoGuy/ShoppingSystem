package UI;

import javax.swing.GroupLayout.Alignment;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

@Route(value = "home", layout = AppLayoutBasic.class)
public class HomeView extends VerticalLayout implements BeforeEnterObserver {

    static boolean isConnected = false;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
        if (!isConnected) {
            connectWebSocket();
            isConnected = true;
        }
    }

    private void connectWebSocket() {
        UI.getCurrent().getPage().executeJs("window.connectWebSocket($0);", getUserId());
    }

    private String getUserId() {
        return VaadinSession.getCurrent().getAttribute("userId").toString();
    }

    @ClientCallable
    public void showNotificationFromJS(String message) {
        System.out.println("Notification from JS: " + message);
        Notification.show(message, 5000, Notification.Position.TOP_CENTER);
    }

    public HomeView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        H1 title = new H1("Welcome to the Home Page!");
        add(title);
    }
}
