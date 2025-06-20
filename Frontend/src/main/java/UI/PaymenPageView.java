package UI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import DTOs.PaymentMethodDTO;

@Route("payment")
public class PaymenPageView extends VerticalLayout implements BeforeEnterObserver {

    private PaymentMethodDTO paymentMethod;

    private double totalAmount = 0.0;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String api;
    private final String paymentMethodUrl;
    private final String checkoutUrl;
    private String currency;
    private String cardNumber;
    private String expirationDateMonth;
    private String expirationDateYear;
    private String cardHolderName;
    private String cvv;
    private String id;

    private String country;
    private String city;
    private String street;
    private String houseNumber;
    private String zipCode;
    private Dialog addressDialog;

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

    public PaymenPageView(@Value("${url.api}") String api, double totalAmount, String country, String city,
            String street, String houseNumber,
            String zipCode, Dialog addressDialog) {

        this.api = api;
        this.paymentMethodUrl = api + "/payment-method";
        this.checkoutUrl = api + "/purchases/checkout";
        this.totalAmount = totalAmount;
        this.country = country;
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
        this.zipCode = zipCode;
        this.addressDialog = addressDialog;

        setUpLayout();
    }

    private PaymentMethodDTO getUserPaymentMethod() {
        String token = getToken();
        String url = paymentMethodUrl + "?authToken=" + token;

        ResponseEntity<PaymentMethodDTO> response = restTemplate.getForEntity(url, PaymentMethodDTO.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            PaymentMethodDTO paymentMethod = response.getBody();
            if (paymentMethod != null) {
                return paymentMethod; // Return the first payment method
            }
        }
        return null; // Handle case where no payment methods are available
    }

    private void setUpLayout() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);

        add(new H1("Payment Page"));
        add(new H3("Order Summary:"));
        add(new H3("Total Amount: $" + totalAmount));

        add(new H3("Please enter payment details below:"));

        TextField currencyField = new TextField("Currency");
        TextField cardNumberField = new TextField("Card Number");
        TextField expirationMonthField = new TextField("Expiration Month (MM)");
        TextField expirationYearField = new TextField("Expiration Year (YYYY)");
        TextField cardHolderNameField = new TextField("Card Holder Name");
        PasswordField cvvField = new PasswordField("CVV");
        TextField idField = new TextField("ID");

        add(currencyField, cardNumberField, expirationMonthField, expirationYearField,
                cardHolderNameField, cvvField, idField);

        // Bind the fields to your variables
        currencyField.addValueChangeListener(e -> currency = e.getValue());
        cardNumberField.addValueChangeListener(e -> cardNumber = e.getValue());
        expirationMonthField.addValueChangeListener(e -> expirationDateMonth = e.getValue());
        expirationYearField.addValueChangeListener(e -> expirationDateYear = e.getValue());
        cardHolderNameField.addValueChangeListener(e -> cardHolderName = e.getValue());
        cvvField.addValueChangeListener(e -> cvv = e.getValue());
        idField.addValueChangeListener(e -> id = e.getValue());

        // Now, create the button outside any "hidden" layout:
        Button payButton = new Button("Pay");

        // Hook the button click to your processPayment() method
        payButton.addClickListener(event -> {
            if (isPaymentDetailsValid()) {
                processPayment();
            } else {
                Notification.show("Please fill in all payment details correctly.");
            }
        });

        // Check if the button should be disabled or enabled (instead of hiding it!)
        Boolean isSuspended = (Boolean) VaadinSession.getCurrent().getAttribute("isSuspended");
        if (Boolean.TRUE.equals(isSuspended)) {
            payButton.setEnabled(false); // Better UX than hiding it
            payButton.setText("Payment not available (account suspended)");
        }

        // Style the button a bit (optional)
        payButton.setWidth("200px");
        payButton.getStyle().set("margin-top", "20px");

        // Finally, add the button directly to your layout — not inside scroller
        add(payButton);
    }

    private void processPayment() {
        try {
            this.paymentMethod = new PaymentMethodDTO(currency, cardNumber, expirationDateMonth,
                    expirationDateYear, cardHolderName, cvv, id);
            String token = getToken();
            String url = checkoutUrl
                    + "?authToken=" + token
                    + "&country=" + country
                    + "&city=" + city
                    + "&street=" + street
                    + "&houseNumber=" + houseNumber
                    + "&zipCode=" + zipCode;
            System.err.println(url);
            HttpEntity<PaymentMethodDTO> entity = new HttpEntity<PaymentMethodDTO>(paymentMethod);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                Notification.show("Payment successful");
                addressDialog.close(); // Close the dialog if payment is successful

            } else {
                Notification.show("Payment failed");
            }
        } catch (Exception e) {
            Notification.show("Payment error");
        }
    }

    private boolean isPaymentDetailsValid() {
        return currency != null && !currency.isEmpty() &&
                cardNumber != null && !cardNumber.isEmpty() &&
                expirationDateMonth != null && !expirationDateMonth.isEmpty() &&
                expirationDateYear != null && !expirationDateYear.isEmpty() &&
                cardHolderName != null && !cardHolderName.isEmpty() &&
                cvv != null && !cvv.isEmpty() &&
                id != null && !id.isEmpty();
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
        String url = api + "/users/"
                + userId + "/isSuspended?token=" + token;
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            VaadinSession.getCurrent().setAttribute("isSuspended", response.getBody());
        } else {
            Notification.show(
                    "Failed to check admin status");
        }
    }
}
