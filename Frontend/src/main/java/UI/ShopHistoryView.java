package UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.History;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

@Route(value = "history", layout = AppLayoutBasic.class)
public class ShopHistoryView extends VerticalLayout implements HasUrlParameter<String>, BeforeEnterObserver {
    private final List<ShopHistoryItem> shopHistoryItems = new ArrayList<>();
    private String shopName;
    private VerticalLayout Reciepts;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    public ShopHistoryView() {
        setSizeFull();

        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Reciepts = new VerticalLayout();
        Reciepts.setWidthFull();
        Reciepts.setAlignItems(Alignment.CENTER);
        Reciepts.setJustifyContentMode(JustifyContentMode.CENTER);
        Reciepts.setSpacing(true);
        Reciepts.setPadding(true);
        add(Reciepts);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.shopName = parameter;

        shopHistoryItems.add(new ShopHistoryItem(
                Map.of("Apple", 3, "Orange", 2, "Banana", 5),
                25.50,
                5.00,
                "2024-01-15",
                "John Doe"));

        shopHistoryItems.add(new ShopHistoryItem(
                Map.of("Milk", 2, "Bread", 1, "Eggs", 12),
                18.75,
                2.50,
                "2024-01-14",
                "Jane Smith"));

        shopHistoryItems.add(new ShopHistoryItem(
                Map.of("Coffee", 1, "Sugar", 2, "Cream", 1),
                15.99,
                1.00,
                "2024-01-13",
                "Bob Wilson"));

        for (ShopHistoryItem item : shopHistoryItems) {
            addItem(item);
        }
    }

    private void addItem(ShopHistoryItem item) {
        // Create a Card component to contain everything
        com.vaadin.flow.component.html.Section card = new com.vaadin.flow.component.html.Section();
        card.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.SMALL,
                LumoUtility.Padding.LARGE);
        card.setWidth("80%");

        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setSpacing(true);
        layout.setPadding(true);

        H3 name = new H3("Buyer: " + item.getBuyerName());
        H3 totalPrice = new H3("Total Price: " + item.getPrice());
        H3 totalDiscount = new H3("Total Discount: " + item.getDiscount());
        H3 date = new H3("Date: " + item.getDate());

        HorizontalLayout lay = new HorizontalLayout();
        lay.add(name);
        lay.add(totalPrice);
        lay.add(totalDiscount);
        lay.add(date);
        lay.setSpacing(true);
        lay.setWidthFull();
        lay.getStyle().set("spacing", "var(--lumo-space-m)");
        lay.getStyle().set("padding", "var(--lumo-space-m)");

        layout.add(lay);

        Grid<Map.Entry<String, Integer>> grid = new Grid<>();
        grid.addColumn(Map.Entry::getKey).setHeader("Item");
        grid.addColumn(Map.Entry::getValue).setHeader("Quantity");
        grid.setItems(item.getItems().entrySet());
        grid.setWidthFull();
        grid.getColumns().forEach(column -> column.setAutoWidth(true));

        Details details = new Details("View Items", grid);
        details.setWidthFull();
        layout.add(details);

        // Add the layout to the card and the card to Receipts
        card.add(layout);
        Reciepts.add(card);
    }

    public static class ShopHistoryItem {
        private Map<String, Integer> items;
        private double price;
        private String date;
        private String buyerName;
        private double discount;

        public ShopHistoryItem(Map<String, Integer> items, double price, double discount, String date,
                String buyerName) {
            this.items = items;
            this.price = price;
            this.date = date;
            this.buyerName = buyerName;
            this.discount = discount;
        }

        public Map<String, Integer> getItems() {
            return items;
        }

        public double getDiscount() {
            return discount;
        }

        public double getPrice() {
            return price;
        }

        public String getDate() {
            return date;
        }

        public String getBuyerName() {
            return buyerName;
        }
    }
}
