package UI;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.LumoUtility;


// tag::snippet[]


public class AppLayoutBasic extends AppLayout implements RouterLayout {

    public AppLayoutBasic() {
        DrawerToggle toggle = new DrawerToggle();

        SideNav nav = getSideNav();

        Scroller scroller = new Scroller(nav);
        scroller.setClassName(LumoUtility.Padding.SMALL);

        addToDrawer(scroller);
        addToNavbar(toggle);
    }
    // end::snippet[]

    private SideNav getSideNav() {
        SideNav sideNav = new SideNav();
        sideNav.addItem(
                new SideNavItem("Home", "/home", VaadinIcon.HOME.create()),
                new SideNavItem("Profile", "/profile", VaadinIcon.USER.create()),
                new SideNavItem("Shopping Cart", "/cart", VaadinIcon.CART.create()),
                new SideNavItem("Search Item", "/items", VaadinIcon.SEARCH.create()),
                new SideNavItem("Search Shop", "/shops", VaadinIcon.SHOP.create()),
                new SideNavItem("My Shops", "/myshops", VaadinIcon.LIST_UL.create()),
                new SideNavItem("My Messages", "/messages", VaadinIcon.ENVELOPE.create()),
                new SideNavItem("Logout", "/logout", VaadinIcon.SIGN_OUT.create())
        );
        return sideNav;
    }

      // tag::snippet[]
}
// end::snippet[]