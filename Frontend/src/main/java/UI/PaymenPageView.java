package UI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.VaadinSession;

import DTOs.PaymentMethodDTO;

public class PaymenPageView extends VerticalLayout implements BeforeEnterObserver {

    private PaymentMethodDTO paymentMethod;

    private double totalAmount = 0.0;
    private final RestTemplate restTemplate = new RestTemplate();

    private String currency;
    private String cardNumber;
    private String expirationDateMonth;
    private String expirationDateYear;
    private String cardHolderName;
    private String cvv;
    private String id;

    private String BASE_URL;

    private String country;
    private String city;
    private String street;
    private String houseNumber;
    private String zipCode;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        getUserId(); // Ensure userId is set in session
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }

        handleSuspence();
    }

    public Integer getUserId() {
        if (VaadinSession.getCurrent().getAttribute("userId") != null) {
            return Integer.parseInt(VaadinSession.getCurrent().getAttribute("userId").toString());
        }
        UI.getCurrent().navigate(""); // Redirect to login if userId is not set
        return null; // Return null if userId is not available
    }

    public PaymenPageView(double totalAmount, String country, String city, String street, String houseNumber,
            String zipCode) {
        this.BASE_URL = "http://localhost:8080/api/";

        this.totalAmount = totalAmount;
        this.country = country;
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
        this.zipCode = zipCode;

        setUpLayout();
    }


    private void setUpLayout() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);

        add(new H1("Payment Page"));
        add(new H3("Order Summary:"));
        add(new H3("Total Amount: $" + totalAmount));
 

        add(new H3("Please enter a payment details below:"));
        TextField currencyField = new TextField("Currency");
        TextField cardNumberField = new TextField("Card Number");
        TextField expirationMonthField = new TextField("Expiration Month (MM)");
        TextField expirationYearField = new TextField("Expiration Year (YYYY)");
        TextField cardHolderNameField = new TextField("Card Holder Name");
        PasswordField cvvField = new PasswordField("CVV");
        TextField idField = new TextField("ID");

        add(currencyField, cardNumberField, expirationMonthField, expirationYearField, cardHolderNameField, cvvField, idField);

        currencyField.addValueChangeListener(e -> currency = e.getValue());
        cardNumberField.addValueChangeListener(e -> cardNumber = e.getValue());
        expirationMonthField.addValueChangeListener(e -> expirationDateMonth = e.getValue());
        expirationYearField.addValueChangeListener(e -> expirationDateYear = e.getValue());
        cardHolderNameField.addValueChangeListener(e -> cardHolderName = e.getValue());
        cvvField.addValueChangeListener(e -> cvv = e.getValue());
        idField.addValueChangeListener(e -> id = e.getValue());

        VerticalLayout buttonLayout = new VerticalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(true);
        buttonLayout.getStyle().setBorder("5px solid #ccc");
        buttonLayout.getStyle().set("border-radius", "10px");
        buttonLayout.getStyle().set("padding", "10px");
        buttonLayout.getStyle().set("background-color", "#f9f9f9");

        Button methodButton = new Button("pay", event -> processPayment());
        if (Boolean.TRUE.equals((Boolean) VaadinSession.getCurrent().getAttribute("isSuspended"))) {
            methodButton.setVisible(false);
        }
        add(methodButton);
        methodButton.setWidthFull();
        buttonLayout.add(methodButton);

        Div scroller = new Div(buttonLayout);
        scroller.getStyle().set("overflow-y", "auto");
        scroller.setHeightFull();
        scroller.setWidth("300px");

        add(scroller);
    }

    private void processPayment() {
        try {
            this.paymentMethod = new PaymentMethodDTO(currency, cardNumber, expirationDateMonth,
                    expirationDateYear, cardHolderName, cvv, id);

            String token = getToken();
            String url = BASE_URL + "purchases/checkout" +
                    "?authToken=" + token +
                    "&country=" + country +
                    "&city=" + city +
                    "&street=" + street +
                    "&houseNumber=" + houseNumber
                    + "&zipCode=" + zipCode +
                    "&currency=" + paymentMethod.toString();

            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                Notification.show("Payment successful");
            } else {
                Notification.show("Payment failed:");
            }
        } catch (Exception e) {
            Notification.show("Payment error");
        }
    }

    private String getToken() {
        return (String) VaadinSession.getCurrent().getAttribute("authToken");
    }

    private void handleSuspence() {
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
