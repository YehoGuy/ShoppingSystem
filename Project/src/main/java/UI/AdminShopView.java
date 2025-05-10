package UI;

import DTOs.ItemDTO;
import DTOs.ShopDTO;
import DomainLayer.Item.ItemCategory;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.*;
import java.util.stream.Collectors;

@Route(value = "admin-shop", layout = AppLayoutBasic.class)
public class AdminShopView extends VerticalLayout {

    private ShopDTO shop;
    private List<ItemDTO> items;
    private Map<ItemDTO, Integer> prices;

    public AdminShopView() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);
        setAlignItems(Alignment.CENTER);

        loadShopData("shop-A");

        H1 title = new H1("🔧 Admin Panel - " + shop.getName());
        add(title);

        add(createItemGrid());
        add(createNewItemForm());
    }

    private void loadShopData(String name) {
        shop = new ShopDTO(
                name,
                new LinkedHashMap<>(Map.of(
                        new ItemDTO(1, "Banana", "Fresh yellow banana", 2.5, ItemCategory.GROCERY), 10,
                        new ItemDTO(2, "Apple", "Juicy red apple", 3.0, ItemCategory.GROCERY), 15
                )),
                new LinkedHashMap<>(Map.of(
                        new ItemDTO(1, "Banana", "Fresh yellow banana", 2.5, ItemCategory.GROCERY), 2,
                        new ItemDTO(2, "Apple", "Juicy red apple", 3.0, ItemCategory.GROCERY), 3
                )),
                List.of()
        );
        items = new ArrayList<>(shop.getItems().keySet());
        prices = new LinkedHashMap<>(shop.getPrices());
    }

    private Grid<ItemDTO> createItemGrid() {
        Grid<ItemDTO> grid = new Grid<>(ItemDTO.class, false);
        grid.setItems(items);

        grid.addColumn(ItemDTO::getName).setHeader("Name").setSortable(true).setAutoWidth(true);
        grid.addColumn(ItemDTO::getDescription).setHeader("Description").setAutoWidth(true);
        grid.addColumn(ItemDTO::getPrice).setHeader("Base Price").setAutoWidth(true);
        grid.addColumn(item -> prices.getOrDefault(item, 0)).setHeader("Selling Price").setKey("sellingPrice");

        grid.addComponentColumn(item -> {
            Button remove = new Button("Remove", e -> {
                shop.getItems().remove(item);
                shop.getPrices().remove(item);
                items.remove(item);
                grid.getDataProvider().refreshAll();
            });
            return remove;
        }).setHeader("Actions");

        // Enable editing price
        Editor<ItemDTO> editor = grid.getEditor();
        NumberField priceField = new NumberField();
        grid.getColumnByKey("sellingPrice").setEditorComponent(priceField);

        grid.addItemDoubleClickListener(e -> {
            editor.editItem(e.getItem());
            priceField.focus();
        });

        editor.setBinder(new com.vaadin.flow.data.binder.Binder<>(ItemDTO.class));
        editor.addSaveListener(event -> {
            double newPrice = priceField.getValue() != null ? priceField.getValue() : 0;
            prices.put(event.getItem(), (int) newPrice);
            shop.getPrices().put(event.getItem(), (int) newPrice);
            grid.getDataProvider().refreshItem(event.getItem());
        });

        return grid;
    }

    private VerticalLayout createNewItemForm() {
        TextField nameField = new TextField("Name");
        TextField descField = new TextField("Description");
        NumberField basePriceField = new NumberField("Base Price");
        ComboBox<ItemCategory> categoryBox = new ComboBox<>("Category", ItemCategory.values());
        NumberField sellPriceField = new NumberField("Selling Price");
        NumberField quantityField = new NumberField("Initial Quantity");

        Button addButton = new Button("Add Item", e -> {
            ItemDTO newItem = new ItemDTO(
                    generateNewItemId(),
                    nameField.getValue(),
                    descField.getValue(),
                    basePriceField.getValue(),
                    categoryBox.getValue()
            );
            shop.getItems().put(newItem, quantityField.getValue().intValue());
            shop.getPrices().put(newItem, sellPriceField.getValue().intValue());
            items.add(newItem);
            prices.put(newItem, sellPriceField.getValue().intValue());
            Notification.show("Item added: " + newItem.getName());
            getUI().ifPresent(ui -> ui.getPage().reload());
        });

        VerticalLayout form = new VerticalLayout(
                nameField, descField, categoryBox,
                basePriceField, sellPriceField, quantityField,
                addButton
        );
        form.setWidth("400px");
        form.setSpacing(true);
        form.setPadding(true);
        form.getStyle().set("border", "1px solid #aaa").set("border-radius", "10px").set("padding", "10px");

        return form;
    }

    private int generateNewItemId() {
        return items.stream().mapToInt(ItemDTO::getId).max().orElse(100) + 1;
    }
}
