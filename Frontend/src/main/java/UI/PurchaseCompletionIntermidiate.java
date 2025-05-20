package UI;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.Map;

import DTOs.AddressDTO;
import DTOs.ItemDTO;

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

    private Map<ItemDTO, Integer> cartItems;
    private double totalPrice = 0;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("");
        }
    }

    public PurchaseCompletionIntermidiate(Map<ItemDTO, Integer> cartItems) {
        this.cartItems = cartItems;
        cartItems.forEach((item, quantity) -> {
            totalPrice += item.getPrice() * quantity;
        });
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

        cartItems.forEach((item, quantity) -> {
            String line = "- " + item.getName() + " x" + quantity + " = $" + (item.getPrice() * quantity);
            add(new Span(line));
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
}
