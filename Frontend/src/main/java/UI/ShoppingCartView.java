package UI;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import DTOs.ItemDTO;
import DTOs.ShoppingCartDTO;
import Domain.ItemCategory;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

@Route(value = "cart", layout = AppLayoutBasic.class)
public class ShoppingCartView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    public ShoppingCartView() {
        // Mock data for demonstration purposes
        // In a real application, you would retrieve this data from a service or
        // database
        ItemDTO item1 = new ItemDTO(1, "Item 1", "Description of Item 1", 10.0, ItemCategory.GROCERY);
        ItemDTO item2 = new ItemDTO(2, "Item 2", "Description of Item 2", 20.0, ItemCategory.GROCERY);
        ItemDTO item3 = new ItemDTO(3, "Item 3", "Description of Item 3", 30.0, ItemCategory.GROCERY);
        ItemDTO item4 = new ItemDTO(4, "Item 4", "Description of Item 4", 40.0, ItemCategory.GROCERY);
        Map<ItemDTO, Integer> items1 = new HashMap<>();
        items1.put(item1, 2);
        items1.put(item2, 1);
        items1.put(item3, 1);
        items1.put(item4, 2);
        Map<ItemDTO, Integer> items2 = new HashMap<>();
        items2.put(item3, 1);
        items2.put(item4, 3);
        items2.put(item1, 1);
        items2.put(item2, 2);
        Map<String, Map<ItemDTO, Integer>> cartItems = new HashMap<>();
        cartItems.put("Shop 1", items1);
        cartItems.put("Shop 2", items2);
        Map<String, Double> shopPrices = new HashMap<>();
        shopPrices.put("Shop 1", 150.0); // Total price for Shop 1
        shopPrices.put("Shop 2", 200.0); // Total price for Shop 2
        double totalPrice = 350.0; // Total price for the entire cart
        ShoppingCartDTO cart = new ShoppingCartDTO(cartItems, totalPrice, shopPrices);
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
                .set("margin", "80px")
                .set("padding", "10px");

        Button buyButton = new Button("Buy entire cart");
        buyButton.addClickListener(event -> {
            PurchaseCompletionIntermidiate purchaseCompletion = new PurchaseCompletionIntermidiate(cart);
            this.removeAll();
            this.add(purchaseCompletion);
        });
        buyButton.getStyle().set("background-color", "red").set("color", "white");
        buyButtonContainer.add(buyButton);

        add(title);

        cart.getCartItems().forEach((shopName, items) -> {
            double shopTotal = cart.getShopPrices().getOrDefault(shopName, 0.0);
            Button buyBasketButton = new Button("Buy basket from " + shopName);
            buyBasketButton.getStyle().set("background-color", "blue").set("color", "white");
            buyBasketButton.addClickListener(event -> {
                // Handle the buy action for this shop's basket
                System.out.println("Buying basket from " + shopName + "...");
            });
            H3 shopHeader = new H3(shopName + " - total price: " + shopTotal + "₪");
            VerticalLayout shopHeaderContainer = new VerticalLayout(shopHeader, buyBasketButton);
            shopHeaderContainer.setWidthFull();
            shopHeaderContainer.setAlignItems(Alignment.CENTER);
            shopHeaderContainer.setJustifyContentMode(JustifyContentMode.BETWEEN);
            add(shopHeaderContainer);
            Grid<ItemRow> grid = new Grid<>(ItemRow.class, false);
            grid.addColumn(ItemRow::name).setHeader("Item name");
            grid.addColumn(ItemRow::price).setHeader("Price");
            grid.addColumn(ItemRow::quantity).setHeader("Quantity");
            grid.addColumn(ItemRow::totalPrice).setHeader("Total price");
            grid.addColumn(ItemRow::description).setHeader("Description");
            grid.setAllRowsVisible(true);
            grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            // Convert items to list of rows
            List<ItemRow> rows = items.entrySet().stream()
                    .map(e -> new ItemRow(e.getKey(), e.getValue()))
                    .toList();

            grid.setItems(rows);
            grid.addComponentColumn(renderer -> {
                Button removeButton = new Button(VaadinIcon.MINUS.create());
                Button addButton = new Button(VaadinIcon.PLUS.create());
                Button removeCompletlyButton = new Button(VaadinIcon.TRASH.create());
                removeCompletlyButton.addClickListener(event -> {
                    System.out.println("Removing all of " + renderer.name() + " from cart...");
                });
                addButton.addClickListener(event -> {
                    System.out.println("Adding " + renderer.name() + " to cart...");
                });
                removeButton.addClickListener(event -> {
                    System.out.println("Removing " + renderer.name() + " from cart...");
                });
                HorizontalLayout buttonLayout = new HorizontalLayout(addButton, removeButton, removeCompletlyButton);
                buttonLayout.setAlignItems(Alignment.CENTER);
                buttonLayout.setSpacing(true);
                return buttonLayout;
            }).setHeader("Actions").setFlexGrow(0).setWidth("200px");
            add(grid);
        });

        HorizontalLayout totalContainer = new HorizontalLayout();
        totalContainer.setAlignItems(Alignment.CENTER);
        totalContainer.getStyle()
                .set("bottom", "0")
                .set("right", "0")
                .set("margin", "0px");
        H2 total = new H2("Total: " + cart.getTotalPrice() + "₪");
        totalContainer.add(total, buyButton);
        add(buyButtonContainer, totalContainer);
    }

    // Helper record class to populate the grid
    public record ItemRow(String name, double price, int quantity, double totalPrice, String description) {
        public ItemRow(ItemDTO item, int quantity) {
            this(item.getName(), item.getPrice(), quantity, item.getPrice() * quantity, item.getDescription());
        }
    }
}