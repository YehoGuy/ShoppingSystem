package UI;

import com.vaadin.flow.router.Route;
import DTOs.PaymentMethodDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.VaadinSession;

import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

public class PaymenPageView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {
    
    private final PaymentMethodDTO paymentMethod;
    private final int id;
    private double totalAmount = 0.0;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String BASE_URL = "http://localhost:8080/api/";
    private String country;
    private String city;
    private String street;
    private String houseNumber;
    private String zipCode;
    private String authToken;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("authToken") == null) {
            event.forwardTo("login");
        }
    }

    public PaymenPageView(int totalAmount, String country, String city, String street, String houseNumber, String zipCode) {

        paymentMethod = getUserPaymentMethod();
        
        this.totalAmount = totalAmount;
        this.country = country;
        this.city = city;
        this.street = street;
        this.houseNumber = houseNumber;
        this.zipCode = zipCode;
        this.authToken = (String) VaadinSession.getCurrent().getAttribute("authToken");


        setUpLayout();
    }


    private PaymentMethodDTO getUserPaymentMethod() {
        String url = BASE_URL + "payment-methods";
        restTemplate.
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

        for (PaymentMethodDTO method : paymentMethods) {
            Button methodButton = new Button(method.getMethodName(), event -> processPayment(method));
            methodButton.setWidthFull();
            buttonLayout.add(methodButton);
        }

        Div scroller = new Div(buttonLayout);
        scroller.getStyle().set("overflow-y", "auto");
        scroller.setHeightFull();
        scroller.setWidth("300px");

        add(scroller);
    }

    private void processPayment(PaymentMethodDTO method) {
        try {
            String token = getToken();
            String country = "USA";
            String city = "New York";
            String street = "5th Ave";
            String houseNumber = "10";

            String url = BASE_URL + "/checkout" +
                    "?authToken=" + token +
                    "&country=" + country +
                    "&city=" + city +
                    "&street=" + street +
                    "&houseNumber=" + houseNumber;

            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                Notification.show("Payment successful: " + method.getMethodName());
            } else {
                Notification.show("Payment failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            Notification.show("Payment error: " + e.getMessage());
        }
    }

    private String getToken() {
        return (String) VaadinSession.getCurrent().getAttribute("authToken");
    }
}
