package UI;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import DTOs.AddressDTO;
import DTOs.ItemDTO;
import DTOs.ShoppingCartDTO;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

public class PurchaseCompletionIntermidiate extends VerticalLayout implements BeforeEnterObserver {

    private ComboBox<String> shippingTypeCombo;
    private TextField countryField;
    private TextField cityField;
    private TextField streetField;
    private TextField houseNumberField;
    private TextField apartmentNumberField;
    private TextField zipCodeField;

    private ShoppingCartDTO cartDto;
    private Map<ItemDTO, Integer> items;
    private double totalPrice = 0;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }

        handleSuspence();
    }

    private String getUserId() {
        return VaadinSession.getCurrent().getAttribute("userId").toString();
    }

    public PurchaseCompletionIntermidiate(ShoppingCartDTO cart) {
        setAllItems();

        this.cartDto = cart;
        this.totalPrice = cart.getTotalPrice();
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        add(new H1("🧾 Complete Your Purchase"));

        setupAddressForm();
        displayCartSummary();

        Button completeButton = new Button("Complete Purchase", event -> {
            AddressDTO address = getAddressFromForm();
            completePurchase(address);
        });
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            completeButton.setVisible(false);
        }
        add(completeButton);
    }

    private void setupAddressForm() {
        countryField = new TextField("Country");
        cityField = new TextField("City");
        streetField = new TextField("Street");
        houseNumberField = new TextField("House Number");
        apartmentNumberField = new TextField("Apartment Number");
        zipCodeField = new TextField("Zip Code");

        shippingTypeCombo = new ComboBox<>("Shipping Type");
        shippingTypeCombo.setItems("Standard", "Express", "Store Pickup");
        shippingTypeCombo.setPlaceholder("Choose one");

        add(new H1("📦 Shipping Address"));
        add(countryField, cityField, streetField, houseNumberField, apartmentNumberField, zipCodeField,
                shippingTypeCombo);
    }

    private AddressDTO getAddressFromForm() {
        String apartmentNumber = apartmentNumberField.isEmpty() ? "0" : apartmentNumberField.getValue();
        return new AddressDTO(countryField.getValue(), cityField.getValue(), streetField.getValue(),
                houseNumberField.getValue(),
                apartmentNumber,
                zipCodeField.getValue());
    }

    private void displayCartSummary() {
        add(new H1("🛒 Cart Summary"));

        cartDto.getShopItemPrices().forEach((shopId, itemPrices) -> {
            itemPrices.forEach((itemId, price) -> {
                ItemDTO item = getItemById(itemId);
                if (item != null) {
                    String line = "- " + item.getName() + " x" + cartDto.getShopItemQuantities().get(shopId).get(itemId)
                            + " = $" + (price * cartDto.getShopItemQuantities().get(shopId).get(itemId));
                    add(new Span(line));
                }
            });
        });

        Span total = new Span("💰 Total: $" + totalPrice);
        total.getStyle().set("font-weight", "bold").set("font-size", "18px");
        add(total);
    }

    private void completePurchase(AddressDTO address) {
        PaymenPageView paymentPage = new PaymenPageView(totalPrice, address.getCountry(), address.getCity(),
                address.getStreet(), address.getHouseNumber(), address.getZipCode());
        Dialog paymentDialog = new Dialog(paymentPage);
        paymentDialog.setWidth("400px");
        paymentDialog.setHeight("300px");
        paymentDialog.add(new Span("Please complete your payment in the dialog."));
        paymentDialog.open();
    }

    private void setAllItems() {
        items = new java.util.HashMap<>();
        cartDto.getShopItemQuantities()
                .forEach((shopId, itemQuantities) -> {
                    itemQuantities.forEach((itemId, quantity) -> {
                        ItemDTO item = cartDto.getItems().stream()
                                .filter(i -> i.getId() == itemId)
                                .findFirst()
                                .orElse(null);
                        if (item != null) {
                            items.put(item, quantity);
                        }
                    });
                });
    }

    private ItemDTO getItemById(int id) {
        return items.keySet().stream()
                .filter(item -> item.getId() == id)
                .findFirst()
                .orElse(null);
    }

    private void handleSuspence() {
        RestTemplate restTemplate = new RestTemplate();

        Integer userId = (Integer) VaadinSession.getCurrent().getAttribute("userId");
        if (userId == null) {
            return;
        }
        String token = (String) VaadinSession.getCurrent().getAttribute("authToken");
        if (token == null) {
            return;
        }
        String url = "http://localhost:8080/api/users" + "/" + userId + "/suspension?token=" + token;
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }

}
