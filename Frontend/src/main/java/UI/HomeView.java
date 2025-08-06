// File: src/main/java/UI/HomeView.java
package UI;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

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

        checkForNotifications();
    }

    private void checkForNotifications() {
        String token = VaadinSession.getCurrent().getAttribute("authToken").toString();
        String url = "http://localhost:8080/api/users/getNotificationsQuantity?token=" + token;
        RestTemplate rest = new RestTemplate();
        ResponseEntity<Integer> quantity = rest.getForEntity(url, Integer.class);
        if (quantity.getStatusCode() == HttpStatus.OK)
            if (quantity.getBody() > 0)
                Notification.show("You have " + quantity.getBody() + " missing notifications.");
    }
}