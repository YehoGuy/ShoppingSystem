package UI;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;

import DTOs.MemberDTO;
import DTOs.ShopDTO;
import UI.AppLayoutBasic;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import com.vaadin.flow.component.UI;

@Route(value = "admin", layout = AppLayoutBasic.class)
public class AdminView extends VerticalLayout implements BeforeEnterObserver {
    public static class UserGridRow {
        private String username;
        private String email;
        private String role;

        public UserGridRow(String username, String email, String role) {
            this.username = username;
            this.email = email;
            this.role = role;
        }

        // Getters and setters required for Vaadin Grid
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class ShopGridRow {
        private String name;
        private String owner;
        private double rating;

        public ShopGridRow(String name, String owner, double rating) {
            this.name = name;
            this.owner = owner;
            this.rating = rating;
        }

        // Getters and setters required for Vaadin Grid
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public double getRating() {
            return rating;
        }

        public void setRating(double rating) {
            this.rating = rating;
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    public AdminView() {
        List<MemberDTO> users = new ArrayList<>();
        List<ShopDTO> shops = new ArrayList<>();
        Map<String, String> owners = new HashMap<>();

        // Sample data for users and shops
        users.add(new MemberDTO(1, "user1", "password1", "email1", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(2, "user2", "password2", "email2", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(3, "user3", "password3", "email3", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(4, "user4", "password4", "email4", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(5, "user5", "password5", "email5", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(6, "user6", "password6", "email6", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(7, "user7", "password7", "email7", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(8, "user8", "password8", "email8", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(9, "user9", "password9", "email9", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(10, "user10", "password10", "email10", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(11, "user11", "password11", "email11", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(12, "user12", "password12", "email12", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(13, "user13", "password13", "email13", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(14, "user14", "password14", "email14", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(15, "user15", "password15", "email15", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(16, "user16", "password16", "email16", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(17, "user17", "password17", "email17", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(18, "user18", "password18", "email18", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(19, "user19", "password19", "email19", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(20, "user20", "password20", "email20", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(21, "user21", "password21", "email21", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(22, "user22", "password22", "email22", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(23, "user23", "password23", "email23", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(24, "user24", "password24", "email24", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(25, "user25", "password25", "email25", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(26, "user26", "password26", "email26", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(27, "user27", "password27", "email27", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(28, "user28", "password28", "email28", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(29, "user29", "password29", "email29", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(30, "user30", "password30", "email30", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(31, "user31", "password31", "email31", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(32, "user32", "password32", "email32", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(33, "user33", "password33", "email33", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(34, "user34", "password34", "email34", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(35, "user35", "password35", "email35", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(36, "user36", "password36", "email36", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(37, "user37", "password37", "email37", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(38, "user38", "password38", "email38", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(39, "user39", "password39", "email39", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(40, "user40", "password40", "email40", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(41, "user41", "password41", "email41", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(42, "user42", "password42", "email42", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(43, "user43", "password43", "email43", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(44, "user44", "password44", "email44", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(45, "user45", "password45", "email45", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(46, "user46", "password46", "email46", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(47, "user47", "password47", "email47", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(48, "user48", "password48", "email48", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        users.add(new MemberDTO(49, "user49", "password49", "email49", "number", new ArrayList<>(), new ArrayList(),
                new ArrayList()));
        List<UserGridRow> userRows = new ArrayList<>();
        for (MemberDTO user : users) {
            userRows.add(new UserGridRow(user.getUsername(), user.getEmail(), "member"));
        }

        shops.add(new ShopDTO("Shop A", new HashMap<>(), new HashMap<>(), new ArrayList<>()));
        shops.add(new ShopDTO("Fashion Boutique", new HashMap<>(), new HashMap<>(), new ArrayList<>()));
        shops.add(new ShopDTO("Home Decor", new HashMap<>(), new HashMap<>(), new ArrayList<>()));
        shops.add(new ShopDTO("Sports Equipment", new HashMap<>(), new HashMap<>(), new ArrayList<>()));
        shops.add(new ShopDTO("Bookstore", new HashMap<>(), new HashMap<>(), new ArrayList<>()));
        shops.add(new ShopDTO("Pet Supplies", new HashMap<>(), new HashMap<>(), new ArrayList<>()));
        shops.add(new ShopDTO("Garden Center", new HashMap<>(), new HashMap<>(), new ArrayList<>()));
        shops.add(new ShopDTO("Music Store", new HashMap<>(), new HashMap<>(), new ArrayList<>()));
        shops.add(new ShopDTO("Toy Store", new HashMap<>(), new HashMap<>(), new ArrayList<>()));
        shops.add(new ShopDTO("Art Supplies", new HashMap<>(), new HashMap<>(), new ArrayList<>()));

        owners.put("Shop A", "user1");
        owners.put("Fashion Boutique", "user2");
        owners.put("Home Decor", "user3");
        owners.put("Sports Equipment", "user4");
        owners.put("Bookstore", "user5");
        owners.put("Pet Supplies", "user6");
        owners.put("Garden Center", "user7");
        owners.put("Music Store", "user8");
        owners.put("Toy Store", "user9");
        owners.put("Art Supplies", "user10");
        List<ShopGridRow> shopRows = new ArrayList<>();
        for (ShopDTO shop : shops) {
            shopRows.add(new ShopGridRow(shop.getName(), owners.get(shop.getName()), 4.5));
        }

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setPadding(true);
        setSpacing(true);

        H1 title = new H1("Welcome to the Admin Panel!");
        add(title);

        H2 usersSection = new H2("System Users");
        add(usersSection);

        // list of all users and the option to give them admin or suspend them
        // list of all users and the option to give them admin or suspend them
        Grid<UserGridRow> userGrid = new Grid<>(UserGridRow.class);
        userGrid.setColumns("username", "email", "role");
        userGrid.addComponentColumn(user -> {
            Button adminButton = new Button("Make Admin");
            Button suspendButton = new Button("Suspend User");
            HorizontalLayout buttons = new HorizontalLayout(adminButton, suspendButton);
            adminButton.addClickListener(e -> {
                // Logic to make user admin
                Notification.show("User " + user.getUsername() + " is now an admin.");
            });
            suspendButton.addClickListener(e -> {
                // Logic to suspend user
                Notification.show("User " + user.getUsername() + " has been suspended.");
            });
            return buttons;
        });
        userGrid.setWidth("100%");
        userGrid.setHeight("350px");
        add(userGrid);

        H2 shopsSection = new H2("System Shops");
        add(shopsSection);

        // list of all shops and the option to remove them
        Grid<ShopGridRow> shopGrid = new Grid<>(ShopGridRow.class);
        shopGrid.setColumns("name", "owner", "rating");
        shopGrid.addComponentColumn(shop -> {
            Button removeButton = new Button("Remove Shop");
            Button viewButton = new Button("View Shop");
            HorizontalLayout buttons = new HorizontalLayout(removeButton, viewButton);
            removeButton.addClickListener(e -> Notification.show("Shop " + shop.getName() + " has been removed."));
            viewButton.addClickListener(e -> UI.getCurrent().navigate("shop/" + shop.getName()));
            return buttons;
        });
        shopGrid.setWidth("100%");
        shopGrid.setHeight("350px");
        add(shopGrid);

        userGrid.setItems(userRows);
        shopGrid.setItems(shopRows);

    }
}
