package UI;


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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;

@JsModule("./notification-client.js")
public class AppLayoutBasic extends AppLayout implements RouterLayout {

    public AppLayoutBasic() {
        UI.getCurrent().getPage().executeJs("window.connectWebSocket($0);", getUserId());
        DrawerToggle toggle = new DrawerToggle();

        SideNav nav = getSideNav();

        Scroller scroller = new Scroller(nav);
        scroller.setClassName(LumoUtility.Padding.SMALL);

        addToDrawer(scroller);
        addToNavbar(toggle);
    }

    private Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    private SideNav getSideNav() {
        String title = "my id:"+getUserId();
        SideNav sideNav = new SideNav();
        sideNav.setLabel(title);
        sideNav.addItem(
                new SideNavItem("Home", "/home", VaadinIcon.HOME.create()),
                new SideNavItem("Profile", "/profile", VaadinIcon.USER.create()),
                new SideNavItem("Shopping Cart", "/cart", VaadinIcon.CART.create()),
                new SideNavItem("Search Item", "/items", VaadinIcon.SEARCH.create()),
                new SideNavItem("Search Shop", "/shops", VaadinIcon.SHOP.create()),
                new SideNavItem("Bids", "/bids", VaadinIcon.MONEY.create()),
                new SideNavItem("Auctions", "/auctions", VaadinIcon.GAVEL.create()),
                new SideNavItem("My Shops", "/myshops", VaadinIcon.LIST_UL.create()),
                new SideNavItem("My Messages", "/messages", VaadinIcon.ENVELOPE.create()));

        // only for admins
        Boolean isAdmin = (Boolean) VaadinSession.getCurrent().getAttribute("isAdmin");
        if (Boolean.TRUE.equals(isAdmin)) {
            sideNav.addItem(new SideNavItem("Admin Panel", "/admin", VaadinIcon.SHIELD.create()));
        }

        sideNav.addItem(new SideNavItem("Logout", "/logout", VaadinIcon.SIGN_OUT.create()));
        return sideNav;
    }

    @ClientCallable
    public void showNotificationFromJS(String message) {
        Notification.show(message, 5000, Notification.Position.TOP_CENTER);
    }

}
