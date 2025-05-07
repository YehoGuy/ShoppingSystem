package UI;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import DTOs.ItemDTO;
import DTOs.ShoppingCartDTO;

@Route("cart")
public class ShoppingCartView extends VerticalLayout {

    public ShoppingCartView(ShoppingCartDTO cart) {
        setSizeFull();
        setSpacing(true);
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        H1 title = new H1("Shopping cart");

        Div buyButtonContainer = new Div();
        buyButtonContainer.getStyle()
            .set("position", "absolute")
            .set("top", "0")
            .set("right", "0")
            .set("margin", "20px");

        Button buyButton = new Button("Buy");
        buyButton.getStyle().set("background-color", "red").set("color", "white");
        buyButtonContainer.add(buyButton);

        add(title, buyButtonContainer);

        cart.getCartItems().forEach((shopName, items) -> {
            double shopTotal = cart.getShopPrices().getOrDefault(shopName, 0.0);

            H3 shopHeader = new H3(shopName + " - total price: " + shopTotal + "₪");
            Grid<ItemRow> grid = new Grid<>(ItemRow.class, false);
            grid.addColumn(ItemRow::name).setHeader("Item name");
            grid.addColumn(ItemRow::price).setHeader("Price");
            grid.addColumn(ItemRow::quantity).setHeader("Quantity");
            grid.addColumn(ItemRow::totalPrice).setHeader("Total price");
            grid.addColumn(ItemRow::description).setHeader("Description");
            grid.setAllRowsVisible(true);
            grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            grid.setWidth("80%");

            // Convert items to list of rows
            List<ItemRow> rows = items.entrySet().stream()
                .map(e -> new ItemRow(e.getKey(), e.getValue()))
                .toList();

            grid.setItems(rows);
            add(shopHeader, grid);
        });

        H2 total = new H2("Total: " + cart.getTotalPrice() + "₪");
        add(total);
    }

    // Helper record class to populate the grid
    public record ItemRow(String name, double price, int quantity, double totalPrice, String description) {
        public ItemRow(ItemDTO item, int quantity) {
            this(item.getName(), item.getPrice(), quantity, item.getPrice() * quantity, item.getDescription());
        }
    }
}