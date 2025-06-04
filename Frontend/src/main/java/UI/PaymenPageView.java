package UI;

import com.vaadin.flow.router.Route;
import DTOs.PaymentMethodDTO;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.server.VaadinSession;

import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;


import java.util.ArrayList;
import java.util.List;

@JsModule("./js/notification-client.js")
public class PaymenPageView extends VerticalLayout implements BeforeEnterObserver {

    private final PaymentMethodDTO paymentMethod;

    private double totalAmount = 0.0;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${url.api}")
    private String BASE_URL;

    private String country;
    private String city;
    private String street;
    private String houseNumber;
    private String zipCode;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }
        UI.getCurrent().getPage().executeJs("import(./js/notification-client.js).then(m -> m.connectNotifications($0))",
                getUserId());
    }

    private String getUserId() {
        return VaadinSession.getCurrent().getAttribute("userId").toString();
    }

    public PaymenPageView(double totalAmount, String country, String city, String street, String houseNumber,
            String zipCode) {

        paymentMethod = getUserPaymentMethod();

        this.totalAmount = totalAmount;
        this.country = country;
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
        this.zipCode = zipCode;

        setUpLayout();
    }

    private PaymentMethodDTO getUserPaymentMethod() {
        String token = getToken();
        String url = BASE_URL + "/payment-method?authToken=" + token;

        ResponseEntity<PaymentMethodDTO> response = restTemplate.getForEntity(url, PaymentMethodDTO.class);
        if (response.getStatusCode() == HttpStatus.OK) {
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
        add(new H3("Shipping Address:"));
        add(new H3("Country: " + this.country));
        add(new H3("City: " + this.city));
        add(new H3("Street: " + this.street));
        add(new H3("House Number: " + this.houseNumber));
        add(new H3("Zip Code: " + this.zipCode));

        add(new H3("Please select a payment method:"));

        VerticalLayout buttonLayout = new VerticalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(true);
        buttonLayout.getStyle().setBorder("5px solid #ccc");
        buttonLayout.getStyle().set("border-radius", "10px");
        buttonLayout.getStyle().set("padding", "10px");
        buttonLayout.getStyle().set("background-color", "#f9f9f9");

        Button methodButton = new Button(paymentMethod.getMethodDetails(), event -> processPayment(paymentMethod));
        methodButton.setWidthFull();
        buttonLayout.add(methodButton);

        Div scroller = new Div(buttonLayout);
        scroller.getStyle().set("overflow-y", "auto");
        scroller.setHeightFull();
        scroller.setWidth("300px");

        add(scroller);
    }

    private void processPayment(PaymentMethodDTO method) {
        try {
            String token = getToken();
            String url = BASE_URL + "/checkout" +
                    "?authToken=" + token +
                    "&country=" + country +
                    "&city=" + city +
                    "&street=" + street +
                    "&houseNumber=" + houseNumber;

            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                Notification.show("Payment successful");
            } else {
                Notification.show("Payment failed:");
            }
        } catch (Exception e) {
            Notification.show("Payment error: " + e.getMessage());
        }
    }

    private String getToken() {
        return (String) VaadinSession.getCurrent().getAttribute("authToken");
    }
}
