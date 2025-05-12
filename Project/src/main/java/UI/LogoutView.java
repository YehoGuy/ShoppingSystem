package UI;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("logout")
public class LogoutView extends VerticalLayout {
    public LogoutView() {
        //call logic to log out the user

        UI.getCurrent().navigate("");
    }

}
