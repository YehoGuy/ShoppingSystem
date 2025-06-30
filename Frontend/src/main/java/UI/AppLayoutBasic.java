package UI;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;

@JsModule("./notification-client.js")
@CssImport("./themes/mytheme/styles.css")
public class AppLayoutBasic extends AppLayout
    implements RouterLayout, BeforeEnterObserver {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;
    
    public AppLayoutBasic(@Value("${url.api}") String api) {
        // addClassName("home-layout");  
        this.baseUrl = api;
        // WebSocket hookup
        UI.getCurrent()
          .getPage()
          .executeJs("window.connectWebSocket($0);", getUserId());

        // Drawer toggle + nav
        DrawerToggle toggle = new DrawerToggle();
        SideNav nav = buildSideNav();
        Scroller scroller = new Scroller(nav);
        scroller.setClassName(LumoUtility.Padding.SMALL);

        addToDrawer(scroller);
        addToNavbar(toggle);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // If we’re heading to /home, mark this layout so the CSS clears its gradient
        boolean isHome = "home".equals(event.getLocation().getFirstSegment());
        if (isHome) {
            addClassName("home-layout");
        } else {
            removeClassName("home-layout");
        }
    }


    private Integer getUserId() {
        Object uid = VaadinSession.getCurrent().getAttribute("userId");
        if (uid != null) {
            return Integer.parseInt(uid.toString());
        }
        UI.getCurrent().navigate(""); // redirect if not logged in
        return null;
    }

    private SideNav buildSideNav() {
        SideNav nav = new SideNav();
        nav.addItem(
            new SideNavItem("Home",        "/home",     VaadinIcon.HOME.create()),
            new SideNavItem("Shopping Cart","/cart",    VaadinIcon.CART.create()),
            new SideNavItem("Search Item", "/items",    VaadinIcon.SEARCH.create()),
            new SideNavItem("Search Shop", "/shops",    VaadinIcon.SHOP.create())
            
        );

        if(!isGuest()) {
            nav.addItem(
                new SideNavItem("Bids",        "/bids",     VaadinIcon.MONEY.create()),//
                new SideNavItem("Auctions",    "/auctions", VaadinIcon.GAVEL.create()),//
                new SideNavItem("My Shops",    "/myshops",  VaadinIcon.LIST_UL.create()),//
                new SideNavItem("Messages",    "/messages", VaadinIcon.ENVELOPE.create()),//
                new SideNavItem("Profile",     "/profile",  VaadinIcon.USER.create())//
            );
           
        }
        Boolean isAdmin = (Boolean) VaadinSession.getCurrent().getAttribute("isAdmin");
        if (Boolean.TRUE.equals(isAdmin)) {
            nav.addItem(new SideNavItem("Admin Panel", "/admin", VaadinIcon.SHIELD.create()));

        }
        nav.addItem(new SideNavItem("Logout", "/logout",
            VaadinIcon.SIGN_OUT.create()));
        return nav;
    }

    @ClientCallable
    public void showNotificationFromJS(String message) {
        Notification.show(message, 5000, Notification.Position.TOP_CENTER);
    }

    private boolean isGuest() {
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            Notification.show("No auth token found. Please log in.", 3000, Notification.Position.MIDDLE);
            return true; // Default to guest if not authenticated
        }

        String url = baseUrl + "/users/isGuest?token=" + token;

        try {
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                Notification.show("Could not determine guest status", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            Notification.show("Error checking guest status", 3000, Notification.Position.MIDDLE);
        }

        return true; // fallback to guest on error
    }

}